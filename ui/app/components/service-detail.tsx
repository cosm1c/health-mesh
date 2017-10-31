import * as React from 'react';
import {Accordion, Button, Panel, Table} from 'react-bootstrap';
import {NodeInfoRecord, ServiceInfoRecord} from '../immutable';
import {bsStyleForHealth} from '../WebSocketJson';

export interface ServiceDetailViewOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

export interface ServiceDetailViewProps {
  selectedNode?: ServiceInfoRecord;
}

export const ServiceDetailView: React.StatelessComponent<ServiceDetailViewOwnProps & ServiceDetailViewProps> = (props) => {
  const {selectedNode, className, style} = props;

  if (!selectedNode) {
    return (<div className={className} style={style}>
      Nothing selected
    </div>);
  }

  return (<Accordion header={selectedNode.serviceName} bsStyle={bsStyleForHealth(selectedNode.healthStatus)}
                     className={className} style={style}>
    {selectedNode.instances.valueSeq().map((instance: NodeInfoRecord) =>
      <Panel key={instance.id} header={selectedNode.serviceName + ' ' + instance.host}
             bsStyle={bsStyleForHealth(instance.healthStatus)}>
        <Table condensed>
          <tbody>
          <tr>
            <td colSpan={2}><Button bsSize='xsmall' onClick={() => fetch(
              `./agents/pollNow/${instance.id}`,
              {method: 'POST'})}>Poll Now</Button></td>
          </tr>
          <tr>
            <th>Host</th>
            <td>{instance.host}</td>
          </tr>
          <tr>
            <th>Health</th>
            <td>{instance.healthStatus}</td>
          </tr>
          <tr>
            <th>Last Updated</th>
            <td>{(new Date(instance.lastUpdatedInstant)).toISOString()}</td>
          </tr>
          {instance.lastPollInstant &&
          <tr>
            <th>Last Polled</th>
            <td>{(new Date(instance.lastPollInstant)).toISOString()}</td>
          </tr>
          }
          {instance.lastPollDurationMillis &&
          <tr>
            <th>Delay</th>
            <td>{instance.lastPollDurationMillis}</td>
          </tr>
          }
          {instance.depends &&
          <tr>
            <th>Depends</th>
            <td>{JSON.stringify(instance.depends)}</td>
          </tr>
          }
          {instance.lastPollResult &&
          <tr>
            <th>Result</th>
            <td>
              <pre>{JSON.stringify(instance.lastPollResult, null, '  ')}</pre>
            </td>
          </tr>
          }
          </tbody>
        </Table>
      </Panel>
    )}
  </Accordion>);
};
