import * as classNames from 'classnames';
import * as React from 'react';
import * as Immutable from 'immutable';
import {connect} from 'react-redux';
import {DataSet, Edge as GraphEdge, Network, Node as GraphNode} from 'vis';
import {colorForHealth, NodeDeltasJson} from '../NodeInfo';
import {NodeInfoRecordMap, WebSocketStateEnum} from '../immutable';
import {ListView} from './';
import {IRootStateRecord} from '../modules';
import {webSocketActionSubject} from '../modules/websocket/epics';
import {DELTA_PAYLOAD, WEBSOCKET_CONNECTED, WebSocketAction} from '../modules/websocket/actions';
import {getWebSocketStateEnum} from '../modules/websocket/selectors';
import {NodeDetailView} from './node-detail';
import {getMetadataIndex} from '../modules/root-selectors';
import {NodeInfoRecord} from '../immutable/NodeInfoRecord';
import {getUserCount} from '../modules/metadata/selectors';
import {websocketStateDisplay} from '../immutable/WebSocketStateRecord';
import {Label, Panel} from 'react-bootstrap';

interface DigraphOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

interface DigraphProps {
  socketState: WebSocketStateEnum;
  index: NodeInfoRecordMap;
  userCount: number;
}

interface DigraphState {
  selection: Array<string>;
  recentlyUpdated: Immutable.Set<string>;
}

const flashDuration = 300;

function nodeInfoFor(payload: NodeDeltasJson, updateType: string, shadow: boolean): Array<GraphNode> {
  const updates = payload[updateType];
  return Object.keys(updates).map(id => {
    const nodeInfoRecord: NodeInfoRecord = updates[id];
    const {backgroundColor, borderColor} = colorForHealth(nodeInfoRecord.healthStatus);
    return {
      id: id,
      label: nodeInfoRecord.label,
      color: backgroundColor,
      border: borderColor,
      shadow: shadow,
    };
  });
}

function arrayConcat<T>(a: Array<T>, b: Array<T>): Array<T> {
  return a.concat(b);
}

function edgesFor(payload: NodeDeltasJson, updateType: string): Array<GraphEdge> {
  const update = payload[updateType];
  return Object.keys(update).map(from => {
    return update[from].depends.map((to: string) => {
      return {
        id: `${from}-${to}`,
        from: from,
        to: to
      };
    });
  }).reduce(arrayConcat, []);
}

class NetworkDigraphComponent extends React.Component<DigraphProps & DigraphOwnProps, DigraphState> {

  private readonly edges = new DataSet<GraphEdge>();
  private readonly nodes = new DataSet<GraphNode>();

  private network: Network;
  private networkEl: HTMLDivElement;
  private firstPacketReceived: boolean = false;
  private initializing: boolean = true;

  constructor(props: DigraphProps) {
    super(props);
    this.state = {
      selection: [],
      recentlyUpdated: Immutable.Set<string>(),
    };

    this.receiveWebSocketAction = this.receiveWebSocketAction.bind(this);
    webSocketActionSubject.subscribe({next: this.receiveWebSocketAction});
  }

  componentDidMount() {
    this.network = new Network(
      this.networkEl,
      {
        edges: this.edges,
        nodes: this.nodes,
      },
      {
        edges: {
          arrows: 'to'
        },
        nodes: {
          shadow: {
            enabled: false,
            color: '#0000FF',
            size: 20,
            x: 0,
            y: 0
          }
        },
        interaction: {
          navigationButtons: true,
          keyboard: true
        }
      });
    this.network.on('selectNode', this.selectionChanged);
    this.network.on('deselectNode', this.selectionChanged);
  }

  private selectionChanged = () =>
    this.changeSelection(this.network.getSelectedNodes() as Array<string>);

  private receiveWebSocketAction(action: WebSocketAction) {
    switch (action.type) {

      case WEBSOCKET_CONNECTED:
        this.edges.clear();
        this.nodes.clear();
        this.firstPacketReceived = false;
        this.initializing = true;
        this.setState({recentlyUpdated: this.state.recentlyUpdated.clear()});
        break;

      case DELTA_PAYLOAD:
        const removedIdsSet: Immutable.Set<string> = Immutable.Set<string>(action.payload.removed);
        this.edges.remove(
          this.edges
            .get({filter: item => removedIdsSet.has(item.from as string) || removedIdsSet.has(item.to as string)})
            .map(i => i.id!));
        this.nodes.remove(action.payload.removed);
        this.nodes.update(nodeInfoFor(action.payload, 'added', !this.initializing));
        this.nodes.update(nodeInfoFor(action.payload, 'updated', !this.initializing));
        this.edges.update(edgesFor(action.payload, 'added'));
        this.edges.update(edgesFor(action.payload, 'updated'));

        if (this.initializing) {
          if (!this.firstPacketReceived) {
            this.firstPacketReceived = true;
            setTimeout(() => {
              this.network.fit();
              this.initializing = false;
            }, 250);
          }

        } else {
          const addedAndUpdatedIds: Immutable.Set<string> = Immutable.Set<string>(
            Object.keys(action.payload.updated).concat(Object.keys(action.payload.added)));

          const newRecentlyUpdated = this.state.recentlyUpdated.withMutations(mutable => {
            action.payload.removed.forEach(id => mutable.delete(id));
            addedAndUpdatedIds.forEach(id => mutable.add(id!));
          });
          if (newRecentlyUpdated !== this.state.recentlyUpdated) {
            this.setState({recentlyUpdated: newRecentlyUpdated});
          }

          setTimeout(() => {
            const oldestFlashInstant = Date.now() - flashDuration;
            const {index} = this.props;
            const {recentlyUpdated} = this.state;

            const culledRecent = recentlyUpdated.withMutations(mutable => {
              addedAndUpdatedIds
                .filter(id => oldestFlashInstant >= index.get(id!).lastUpdatedInstant)
                .forEach(id => {
                  mutable.delete(id!);
                  this.nodes.update({
                    id: id,
                    shadow: false
                  } as GraphNode);
                });
            });
            if (culledRecent !== recentlyUpdated) {
              this.setState({recentlyUpdated: culledRecent});
            }

          }, flashDuration);
        }
        break;
    }
  }

  private changeSelection = (ids: Array<string>) => {
    this.setState({
      selection: ids
    });

    if (ids.length > 0) {
      const selectedNodes = this.network.getSelectedNodes();
      if (selectedNodes.length !== ids.length || selectedNodes.every((v, i) => v !== ids[i])) {
        this.network.focus(ids[0], {animation: true});
        this.network.setSelection({nodes: ids, edges: []});
      }
    }
  };

  private static websocketLabel(socketState: WebSocketStateEnum) {
    switch (socketState) {
      case WebSocketStateEnum.DISCONNECTED:
        return (<Label bsStyle='danger'>Disconnected</Label>);
      case WebSocketStateEnum.CONNECTING:
        return (<Label bsStyle='info'>Connecting</Label>);
      case WebSocketStateEnum.CONNECTED:
        return (<Label bsStyle='success'>Connected</Label>);
      case WebSocketStateEnum.DISCONNECTING:
        return (<Label bsStyle='default'>Disconnecting</Label>);
      default:
        return (<Label bsStyle='warning'>Unknown {websocketStateDisplay(socketState)}</Label>);
    }
  }

  render() {
    const {className, style, index, socketState, userCount} = this.props;
    const digraphClass = classNames(className, 'digraph');

    return (<div className={digraphClass} style={style}>
      <Panel className='statusbar'>
        {NetworkDigraphComponent.websocketLabel(socketState)}
        <Label bsStyle='info'>User Count: {userCount}</Label>
        <Label bsStyle='info'>Nodes: {this.nodes.length}</Label>
        <Label bsStyle='info'>Edges: {this.edges.length}</Label>
      </Panel>

      <div className='digraph-inner'>
        <div className='digraph-controls'>
          <NodeDetailView className='digraph-detailview'
                          selectedNode={this.state.selection.length > 0 ? index.get(this.state.selection[0]) : undefined}/>
          <ListView className='digraph-listview'
                    changeSelection={this.changeSelection}
                    selection={this.state.selection}
                    recentlyUpdated={this.state.recentlyUpdated}
                    nodeInfoRecordMap={index}/>
        </div>

        <div className='digraph-network' ref={(divEl) => divEl ? this.networkEl = divEl : null}/>
      </div>
    </div>);
  }
}

const mapStateToProps: (state: IRootStateRecord) => DigraphProps =
  (state: IRootStateRecord) => ({
    socketState: getWebSocketStateEnum(state),
    index: getMetadataIndex(state),
    userCount: getUserCount(state)
  });

export const Digraph = connect<DigraphProps, DigraphState, DigraphOwnProps>(mapStateToProps)(NetworkDigraphComponent);
