import {Subject} from 'rxjs/Subject';
import {Data as GraphData, Edge as GraphEdge, Node as GraphNode} from 'vis';
import {ServiceInfoRecord, ServicesRecordMap} from '../../immutable';
import {graphColorForHealth} from '../../WebSocketJson';

function hasChanged(x: ServiceInfoRecord, y: ServiceInfoRecord): boolean {
  return x.healthStatus !== y.healthStatus || !x.depends.equals(y.depends);
}

function createEdge(from: string, to: string): GraphEdge {
  return {
    id: `${from}-${to}`,
    from: from,
    to: to
  };
}

export function publishDigraphDelta(servicesToCheck: Set<string>,
                                    prevIndex: ServicesRecordMap,
                                    updatedIndex: ServicesRecordMap) {
  const removedNodes: Set<string> = new Set<string>();
  const removedEdges: string[] = [];
  const nodes: GraphNode[] = [];
  const edges: GraphEdge[] = [];

  servicesToCheck.forEach(serviceName => {
    const prevServiceValue = prevIndex.get(serviceName);
    const currServiceValue = updatedIndex.get(serviceName);

    if ((prevServiceValue && !currServiceValue) || (currServiceValue.instances.size === 0)) {
      removedNodes.add(serviceName);

    } else if (!prevServiceValue && currServiceValue) {
      nodes.push({
        id: currServiceValue.serviceName,
        label: currServiceValue.serviceName,
        color: graphColorForHealth(currServiceValue.healthStatus),
      });

      edges.push(
        ...currServiceValue.depends
          .map((to: string) => createEdge(currServiceValue.serviceName, to))
          .toArray()
      );

    } else if (hasChanged(prevServiceValue, currServiceValue)) {
      const addedDepends = currServiceValue.depends.subtract(prevServiceValue.depends);
      const removedDepends = currServiceValue.depends.subtract(prevServiceValue.depends);

      edges.push(
        ...addedDepends
          .map((to: string) => createEdge(currServiceValue.serviceName, to))
          .toArray()
      );
      removedEdges.push(
        ...removedDepends
          .map((to: string) => `${currServiceValue.serviceName}-${to}`)
          .toArray()
      );
    }
  });

  digraphDeltaSubject.next({
    removedEdges,
    removedNodes,
    updated: {
      nodes,
      edges,
    },
  });
}

export interface DigraphDelta {
  readonly removedEdges: Array<string>;
  readonly removedNodes: Set<string>;
  readonly updated: GraphData;
}

export const digraphDeltaSubject: Subject<DigraphDelta> = new Subject();
