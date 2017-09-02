import * as classNames from 'classnames';
import * as React from 'react';
import * as Immutable from 'immutable';
import {connect} from 'react-redux';
import {DataSet, Edge as GraphEdge, Network, Node as GraphNode} from 'vis';
import {colorForHealth, NodeDeltasJson} from '../NodeInfo';
import {NodeInfoRecordMap, WebSocketStateEnum} from '../immutable';
import {ListView, StatusBar} from './';
import {IRootStateRecord} from '../modules';
import {webSocketActionSubject} from '../modules/websocket/epics';
import {WEBSOCKET_CONNECTED, DELTA_PAYLOAD, WebSocketAction} from '../modules/websocket/actions';
import {getWebSocketStateEnum} from '../modules/websocket/selectors';
import {NodeDetailView} from './node-detail';
import {getMetadataIndex} from '../modules/root-selectors';
import {NodeInfoRecord} from '../immutable/NodeInfoRecord';
import {getUserCount} from '../modules/metadata/selectors';

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

function nodeInfoFor(payload: NodeDeltasJson, updateType: string): Array<GraphNode> {
  const updates = payload[updateType];
  return Object.keys(updates).map(id => {
    const nodeState: NodeInfoRecord = updates[id];
    const {backgroundColor, borderColor} = colorForHealth(nodeState.healthStatus);
    return {
      id: id,
      label: nodeState.label,
      color: backgroundColor,
      border: borderColor,
      shadow: true,
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
        this.setState({recentlyUpdated: this.state.recentlyUpdated.clear()});
        break;

      case DELTA_PAYLOAD:
        const removedIdsSet: Immutable.Set<string> = Immutable.Set<string>(action.payload.removed);
        this.edges.remove(
          this.edges
            .get({filter: item => removedIdsSet.has(item.from as string) || removedIdsSet.has(item.to as string)})
            .map(i => i.id!));
        this.nodes.remove(action.payload.removed);
        this.nodes.update(nodeInfoFor(action.payload, 'added'));
        this.nodes.update(nodeInfoFor(action.payload, 'updated'));
        this.edges.update(edgesFor(action.payload, 'added'));
        this.edges.update(edgesFor(action.payload, 'updated'));

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

  render() {
    const {className, style, index, socketState, userCount} = this.props;
    const digraphClass = classNames(className, 'digraph');

    return (<div className={digraphClass} style={style}>
      <StatusBar className='digraph-statusbar'
                 nodeCount={this.nodes.length}
                 edgeCount={this.edges.length}
                 socketState={socketState}
                 userCount={userCount}/>
      <div className='digraph-network' ref={(divEl) => divEl ? this.networkEl = divEl : null}/>

      <div className='detail-view'>
        <NodeDetailView className='digraph-detailview'
                        selectedNode={this.state.selection.length > 0 ? index.get(this.state.selection[0]) : undefined}/>
        <ListView className='digraph-listview'
                  changeSelection={this.changeSelection}
                  selection={this.state.selection}
                  recentlyUpdated={this.state.recentlyUpdated}
                  nodeInfoRecordMap={index}/>
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
