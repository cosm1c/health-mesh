import * as classNames from 'classnames';
import * as React from 'react';
import {connect} from 'react-redux';
import {DataSet, Edge as GraphEdge, Network, Node as GraphNode} from 'vis';
import {NodeDeltasJson, NodeState} from '../NodeInfo';
import {NodeInfoMap, WebSocketStateEnum} from '../immutable';
import {ListView, StatusBar} from './';
import {IRootStateRecord} from '../modules';
import {getMetadataIndex} from '../modules/metadata/selectors';
import {webSocketActionSubject} from '../modules/websocket/epics';
import {WEBSOCKET_CONNECTED, WEBSOCKET_PAYLOAD, WebSocketAction} from '../modules/websocket/actions';
import {getWebSocketStateEnum} from '../modules/websocket/selectors';

interface DigraphOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

interface DigraphProps {
  socketState: WebSocketStateEnum;
  index: NodeInfoMap;
}

interface DigraphState {
  selection: Array<string>;
}

function nodeInfoFor(payload: NodeDeltasJson, updateType: string): Array<GraphNode> {
  return Object.keys(payload[updateType]).map(id => {
    const nodeState: NodeState = payload[updateType][id];
    return {
      id: id,
      label: nodeState.label,
      color: nodeState.cssHexColor,
    };
  });
}

function arrayConcat<T>(a: Array<T>, b: Array<T>): Array<T> {
  return a.concat(b);
}

function edgesFor(payload: NodeDeltasJson, updateType: string): Array<GraphEdge> {
  return Object.keys(payload[updateType]).map(from => {
    return payload[updateType][from].depends.map((to: string) => {
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
      selection: []
    };

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
        }
      });
    this.network.on('selectNode', this.selectionChanged);
    this.network.on('deselectNode', this.selectionChanged);
  }

  private selectionChanged = () =>
    this.changeSelection(this.network.getSelectedNodes() as Array<string>);

  private receiveWebSocketAction = (action: WebSocketAction) => {
    switch (action.type) {

      case WEBSOCKET_CONNECTED:
        this.edges.clear();
        this.nodes.clear();
        break;

      case WEBSOCKET_PAYLOAD:
        const removedIdsSet = new Set<string>(action.payload.removed);
        this.edges.remove(
          this.edges
            .get({filter: item => removedIdsSet.has(item.from as string) || removedIdsSet.has(item.to as string)})
            .map(i => i.id!));
        this.nodes.remove(action.payload.removed);
        this.nodes.update(nodeInfoFor(action.payload, 'added'));
        this.nodes.update(nodeInfoFor(action.payload, 'updated'));
        this.edges.update(edgesFor(action.payload, 'added'));
        this.edges.update(edgesFor(action.payload, 'updated'));
        break;
    }
  };

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
    const {className, style, index, socketState} = this.props;
    const digraphClass = classNames(className, 'digraph');

    return (<div className={digraphClass} style={style}>
      <StatusBar className='digraph-statusbar'
                 nodeCount={this.nodes.length}
                 edgeCount={this.edges.length}
                 socketState={socketState}/>
      <div className='digraph-network' ref={(divEl) => divEl ? this.networkEl = divEl : null}/>
      <ListView className='digraph-listview'
                changeSelection={this.changeSelection}
                selection={this.state.selection}
                nodeInfoMap={index}/>
    </div>);
  }
}

const mapStateToProps: (state: IRootStateRecord) => DigraphProps =
  (state: IRootStateRecord) => ({
    socketState: getWebSocketStateEnum(state),
    index: getMetadataIndex(state),
  });

export const Digraph = connect<DigraphProps, DigraphState, DigraphOwnProps>(mapStateToProps)(NetworkDigraphComponent);
