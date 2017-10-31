import * as classNames from 'classnames';
import * as React from 'react';
import {connect} from 'react-redux';
import {DataSet, Edge as GraphEdge, Network, Node as GraphNode} from 'vis';
import {ServicesRecordMap, WebSocketStateEnum} from '../immutable';
import {ListView} from './';
import {IRootStateRecord} from '../modules';
import {WEBSOCKET_CONNECTED, WebSocketAction} from '../modules/websocket/actions';
import {getWebSocketStateEnum} from '../modules/websocket/selectors';
import {ServiceDetailView} from './service-detail';
import {getMetadataIndex} from '../modules/root-selectors';
import {getUserCount} from '../modules/metadata/selectors';
import {websocketStateDisplay} from '../immutable/WebSocketStateRecord';
import {Label, Panel} from 'react-bootstrap';
import {DigraphDelta, digraphDeltaSubject} from '../modules/digraph/DigraphSubject';
import {webSocketActionSubject} from '../modules/websocket/epics';

interface DigraphOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

interface DigraphProps {
  socketState: WebSocketStateEnum;
  index: ServicesRecordMap;
  userCount: number;
}

interface DigraphState {
  selection: Array<string>;
}

class NetworkDigraphComponent extends React.Component<DigraphProps & DigraphOwnProps, DigraphState> {

  private readonly edges = new DataSet<GraphEdge>();
  private readonly nodes = new DataSet<GraphNode>();

  private network: Network;
  private networkEl: HTMLDivElement;
  private timeout: number | null = null;
  private firstPacketReceived = false;

  constructor(props: DigraphProps) {
    super(props);
    this.state = {
      selection: [],
    };

    this.receiveWebSocketAction = this.receiveWebSocketAction.bind(this);
    webSocketActionSubject.subscribe({next: this.receiveWebSocketAction});
    this.receiveDigraphDelta = this.receiveDigraphDelta.bind(this);
    digraphDeltaSubject.subscribe({next: this.receiveDigraphDelta});
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

  private autoFitDigraph() {
    if (this.timeout) window.clearTimeout(this.timeout);
    this.timeout = window.setTimeout(() => {
      this.network.fit();
    }, 1000);
  }

  private receiveWebSocketAction(action: WebSocketAction) {
    switch (action.type) {
      case WEBSOCKET_CONNECTED:
        // this.edges.clear();
        // this.nodes.clear();
        // this.props.index.clear();
        this.firstPacketReceived = false;
        this.autoFitDigraph();
        break;
    }
  }

  private receiveDigraphDelta(delta: DigraphDelta) {
    this.edges.remove(delta.removedEdges);
    this.edges.remove(
      this.edges
        .get({filter: item => delta.removedNodes.has(item.from as string) || delta.removedNodes.has(item.to as string)})
        .map(i => i.id!));
    this.nodes.remove(Array.from(delta.removedNodes));

    this.nodes.update(delta.updated.nodes as GraphNode[]);
    this.edges.update(delta.updated.edges as GraphEdge[]);

    if (!this.firstPacketReceived) {
      this.firstPacketReceived = true;
      this.network.fit();
      this.autoFitDigraph();
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
        <Label bsStyle='info'>Instances: {
          index.valueSeq()
            .map(serviceInfo => serviceInfo!.instances.size)
            .reduce((a, b) => a! + b!, 0)
        }</Label>
      </Panel>

      <div className='digraph-inner'>
        <div className='digraph-controls'>
          <ServiceDetailView className='digraph-detailview'
                             selectedNode={this.state.selection.length > 0 ? index.get(this.state.selection[0]) : undefined}/>
          <ListView className='digraph-listview'
                    changeSelection={this.changeSelection}
                    selection={this.state.selection}
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
