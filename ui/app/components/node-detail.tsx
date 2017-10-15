import * as React from 'react';
import {NodeInfoRecord} from '../immutable/NodeInfoRecord';
import {Button, Panel, Table} from 'react-bootstrap';

export interface NodeDetailViewOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

export interface NodeDetailViewProps {
  selectedNode?: NodeInfoRecord;
}

export const NodeDetailView: React.StatelessComponent<NodeDetailViewOwnProps & NodeDetailViewProps> = (props) => {
  const {selectedNode, className, style} = props;

  if (!selectedNode) {
    return (<div className={className} style={style}>
      Nothing selected
    </div>);
  }

  return (<Panel className={className} style={style}>
    <Table condensed>
      <tbody>
      <tr>
        <td colSpan={2}><Button onClick={() => fetch(
          `/health-mesh/agents/pollNow/${selectedNode.id}`,
          {method: 'POST'})}>Poll Now</Button></td>
      </tr>
      <tr>
        <th>Label</th>
        <td>{selectedNode.label}</td>
      </tr>
      <tr>
        <th>Health</th>
        <td>{selectedNode.healthStatus}</td>
      </tr>
      <tr>
        <th>Last Updated</th>
        <td>{(new Date(selectedNode.lastUpdatedInstant)).toISOString()}</td>
      </tr>
      {selectedNode.lastPollInstant &&
      <tr>
        <th>Last Polled</th>
        <td>{(new Date(selectedNode.lastPollInstant)).toISOString()}</td>
      </tr>
      }
      {selectedNode.lastPollDurationMillis &&
      <tr>
        <th>Delay</th>
        <td>{selectedNode.lastPollDurationMillis}</td>
      </tr>
      }
      {selectedNode.lastPollResult &&
      <tr>
        <th>Result</th>
        <td>
          <pre>{selectedNode.lastPollResult}</pre>
        </td>
      </tr>
      }
      </tbody>
    </Table>
  </Panel>);
};
