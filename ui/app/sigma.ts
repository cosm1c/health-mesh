/// <reference path="../typings/index.d.ts" />
require('sigma');
require('sigma/build/plugins/sigma.layout.forceAtlas2.min');
import {NodeInfo} from './NodeInfo';
import Sigma = SigmaJs.Sigma;

export class SigmaDigraph {

  private static readonly globalNodeId: string = 'GLOBAL';
  private static readonly nodeRows: number = 5;
  private static readonly layoutDelayMs: number = 200;
  private static readonly layoutDurationMs: number = 2000;

  private readonly s: Sigma = new sigma({
    settings: {
      edgeColor: 'source'
    }
  });

  private nodeCount: number = 0;
  private topologyChanged = false;
  private layoutTimeout: number | null = null;

  constructor(digraphEl: HTMLElement) {
    this.nodeAddOrUpdate(SigmaDigraph.globalNodeId, SigmaDigraph.globalNodeId, 1, 'black');

    this.s.addRenderer({
      container: digraphEl,
      type: 'webgl'
    });
  }

  private performLayout() {
    if (this.layoutTimeout) {
      console.debug('Clearing existing layout timeout');
      clearTimeout(this.layoutTimeout);
      this.layoutTimeout = null;
    }

    if (this.s.isForceAtlas2Running()) {
      console.debug('Killing layout due to new layout request');
      this.s.killForceAtlas2();
    }

    console.debug(`Scheduling layout after ${SigmaDigraph.layoutDelayMs}ms delay`);
    this.layoutTimeout = setTimeout(() => {

      if (!this.s.isForceAtlas2Running()) {
        console.debug(`Starting scheduled layout after ${SigmaDigraph.layoutDelayMs}ms delay - will kill after ${SigmaDigraph.layoutDurationMs / 1000}s`);
        this.s.startForceAtlas2({worker: true, barnesHutOptimize: true});
      }

      this.layoutTimeout = setTimeout(() => {
        console.debug(`Killing layout after ${SigmaDigraph.layoutDurationMs}ms duration`);
        this.s.killForceAtlas2();
        this.layoutTimeout = null;
      }, SigmaDigraph.layoutDurationMs);

    }, SigmaDigraph.layoutDelayMs);
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
      this.nodeAddOrUpdate(nodeInfo.id, nodeInfo.label, 1, SigmaDigraph.colorForNode(nodeInfo));
      if (nodeInfo.depends.length === 0) {
        // For testing - if no depends then link to Global
        this.addEdgeIfNotExist(nodeInfo.id, SigmaDigraph.globalNodeId);
      }
      nodeInfo.depends.forEach((dependId: string) => {
        this.nodeAddOrUpdate(dependId, dependId, 1, '#a9a9a9');
        this.addEdgeIfNotExist(nodeInfo.id, dependId);
      });
    });

    if (this.topologyChanged) {
      this.topologyChanged = false;

      if (this.s.isForceAtlas2Running()) {
        console.debug('Restarting layout due to topology change');
        this.s.killForceAtlas2();
      }

      this.performLayout();
    }

    this.s.refresh();
  }

  private nodeAddOrUpdate(id: string, label: string, size: number, color: string) {
    const node = this.s.graph.nodes(id);
    if (!node) {
      this.nodeCount++;
      this.topologyChanged = true;
      this.s.graph.addNode({
        id: id,
        label: label,
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
