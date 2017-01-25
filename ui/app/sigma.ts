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
          size: 5
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
    /*
     this.dragListener.bind('dragend', () => {
     if (!this.s.isForceAtlas2Running()) {
     this.s.startForceAtlas2({worker: true, barnesHutOptimize: true});
     }
     });
     */
  }

  static colorForNode(nodeInfo: NodeInfo): string {
    switch (nodeInfo.healthStatus) {
      case 'unhealthy':
        return '#ff0000';
      case 'healthy':
        return '#00FF00';
      default:
        return '#ffff00';
    }
  }

  update(add: NodeInfo[], del: NodeInfo[]) {
    console.debug(`${new Date()}\n add:`, add, ' del:', del);
    console.debug(`isForceAtlas2Running = ${this.s.isForceAtlas2Running()}`);

    let nodesAddedOrRemoved = false;

    add.forEach((node: NodeInfo) => {
      const id = `n-${node.id}`,
        existingNode = this.s.graph.nodes(id);

      if (existingNode) {
        existingNode.color = SigmaDigraph.colorForNode(node);

      } else {
        nodesAddedOrRemoved = true;
        this.nodeCount++;
        this.s.graph.addNode({
          id: id,
          label: node.id,
          color: SigmaDigraph.colorForNode(node),
          x: this.nodeCount % SigmaDigraph.nodeRows,
          y: this.nodeCount,
          size: 3
        });
        // Link back to root node (lr somewhere else in a hierarchy) - prevents orphans
        this.s.graph.addEdge({
          id: `e-${node.id}-${SigmaDigraph.rootNodeID}`,
          color: SigmaDigraph.colorForNode(node),
          source: id,
          target: SigmaDigraph.rootNodeID
        });
      }
    });

    add.forEach((node: NodeInfo) => {
      node.depends.forEach((targetId: string) => {
        const id = `e-${node.id}-${targetId}`,
          existingNode = this.s.graph.edges(id);

        if (existingNode) {
          existingNode.color = SigmaDigraph.colorForNode(node);

        } else {
          this.s.graph.addEdge({
            id: id,
            color: SigmaDigraph.colorForNode(node),
            source: `n-${node.id}`,
            target: `n-${targetId}`
          });
        }
      });
    });

    if (nodesAddedOrRemoved) {
      if (this.s.isForceAtlas2Running()) {
        this.s.killForceAtlas2();
      }
      this.s.startForceAtlas2({worker: true, barnesHutOptimize: true});
    }

    this.s.refresh();
  }

}
