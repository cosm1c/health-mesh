/// <reference path="../typings/index.d.ts" />
require('sigma');
require('sigma/build/plugins/sigma.layout.forceAtlas2.min');
require('sigma/build/plugins/sigma.plugins.dragNodes.min');
import {NodeInfo} from './NodeInfo';
import Sigma = SigmaJs.Sigma;

export class SigmaDigraph {

  private static readonly nodeRows: number = 5;
  private static readonly rootNodeID: string = 'root';

  private s: Sigma;
  private nodeCount: number = 0;
  private dragListener: SigmaJs.DragNodes;
  private topologyChanged = false;

  constructor(digraphEl: HTMLElement) {
    this.s = new sigma({
      settings: {
        edgeColor: 'source',
        // defaultNodeColor: 'node'
        // minArrowSize
      },
      graph: {
        edges: [],
        nodes: [{
          id: SigmaDigraph.rootNodeID,
          x: 0,
          y: 0,
          size: 3
        }]
      }
    });

    this.s.addRenderer({
      container: digraphEl/*, // webgl incompatible with dragNodes
       type: 'webgl'*/
    });

    this.dragListener = sigma.plugins.dragNodes(this.s, this.s.renderers[0]);
    this.dragListener.bind('startdrag', () => {
      if (this.s.isForceAtlas2Running()) {
        this.s.killForceAtlas2();
      }
    });
    this.dragListener.bind('dragend', () => {
      if (!this.s.isForceAtlas2Running()) {
        this.s.startForceAtlas2({worker: true, barnesHutOptimize: true});
      }
    });
  }

  static colorForNode(nodeInfo: NodeInfo): string {
    switch (nodeInfo.healthStatus) {
      case 'unhealthy':
        return '#ff0000';
      case 'healthy':
        return '#00FF00';
      default:
        return '#ffa500';
    }
  }

  update(add: NodeInfo[], del: NodeInfo[]) {
    console.debug(`${new Date()}\n add:`, add, ' del:', del);
    // console.debug(`isForceAtlas2Running = ${this.s.isForceAtlas2Running()}`);

    add.forEach((nodeInfo: NodeInfo) => {
      this.nodeAddOrUpdate(nodeInfo.id, 1, SigmaDigraph.colorForNode(nodeInfo));
      this.addEdgeIfNotExist(nodeInfo.id, SigmaDigraph.rootNodeID);
      nodeInfo.depends.forEach((dependId: string) => {
        this.nodeAddOrUpdate(dependId, 1, '#d3d3d3');
        this.addEdgeIfNotExist(nodeInfo.id, dependId);
      });
    });

    if (this.topologyChanged) {
      if (this.s.isForceAtlas2Running()) {
        this.s.killForceAtlas2();
      }
      this.s.startForceAtlas2({worker: true, barnesHutOptimize: true});
      this.topologyChanged = false;
    }

    this.s.refresh();
  }

  private nodeAddOrUpdate(id: string, size: number, color: string) {
    const node = this.s.graph.nodes(id);
    if (!node) {
      this.nodeCount++;
      this.topologyChanged = true;
      this.s.graph.addNode({
        id: id,
        label: id,
        x: this.nodeCount % SigmaDigraph.nodeRows,
        y: Math.floor(this.nodeCount / SigmaDigraph.nodeRows),
        size: size,
        color: color
      });

    } else if (node.color !== color) {
      node.color = color;
    }
  }

  private addEdgeIfNotExist(source: string, target: string) {
    const id = `e-${source}-${target}`;
    if (!this.s.graph.edges(id)) {
      this.topologyChanged = true;
      this.s.graph.addEdge({
        id: id,
        source: source,
        target: target,
        size: 1
      });
    }
  }
}
