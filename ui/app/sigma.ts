/// <reference path="../typings/index.d.ts" />
require('sigma');
require('sigma/build/plugins/sigma.layout.forceAtlas2.min');
require('sigma/build/plugins/sigma.plugins.dragNodes.min');
import {NodeInfo} from './NodeInfo';
import Sigma = SigmaJs.Sigma;

export class SigmaDigraph {

  private static readonly nodeRows: number = 5;
  private static readonly layoutDelayMs: number = 200;
  private static readonly layoutDurationMs: number = 2000;

  private readonly s: Sigma = new sigma({
    settings: {
      edgeColor: 'source'
    }
  });

  private nodeCount: number = 0;
  private dragListener: SigmaJs.DragNodes;
  private topologyChanged = false;
  private layoutTimeout: number | null = null;

  constructor(digraphEl: HTMLElement) {
    this.s.addCamera('cam1');

    this.s.addRenderer({
      container: digraphEl,
      camera: 'cam1' /*, // webgl incompatible with dragNodes
       type: 'webgl'*/
    });

    this.dragListener = sigma.plugins.dragNodes(this.s, this.s.renderers[0]);
    this.dragListener.bind('startdrag', () => {
      if (this.s.isForceAtlas2Running()) {
        this.s.stopForceAtlas2();
      }
    });
    this.dragListener.bind('dragend', () => {
      this.performLayout();
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
      this.nodeAddOrUpdate(nodeInfo.id, 1, SigmaDigraph.colorForNode(nodeInfo));
      nodeInfo.depends.forEach((dependId: string) => {
        this.nodeAddOrUpdate(dependId, 1, '#a9a9a9');
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
