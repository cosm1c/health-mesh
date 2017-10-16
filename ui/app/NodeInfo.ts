// TypeScript version of Scala Case Class ExampleAgent.ExampleAgentWebsocketPayload
export interface NodeStateJson {
  readonly label: string;
  readonly depends: Array<string>;
  readonly healthStatus: string;
  readonly lastPollInstant?: number;
  readonly lastPollDurationMillis?: number;
  readonly lastPollResult?: any;
}

export interface NodeCollectionJson {
  [id: string]: NodeStateJson;
}

export interface NodeDeltasJson {
  readonly added: NodeCollectionJson;
  readonly updated: NodeCollectionJson;
  readonly removed: Array<string>;
}

export interface UserCountJson {
  readonly userCount: number;
}

export function bsStyleForHealth(healthStatus: string): string {
  switch (healthStatus) {
    case 'Unknown':
      return 'warning';
    case 'Healthy':
      return 'success';
    case 'Unhealthy':
      return 'danger';
    default:
      return 'info';
  }
}

export function colorForHealth(healthStatus: string): { color: string, backgroundColor: string, borderColor: string } {
  switch (healthStatus) {
    case 'Unknown':
      return {
        color: '#856404',
        backgroundColor: '#fff3cd',
        borderColor: '#ffeeba',
      };
    case 'Healthy':
      return {
        color: '#155724',
        backgroundColor: '#d4edda',
        borderColor: '#c3e6cb',
      };
    case 'Unhealthy':
      return {
        color: '#721c24',
        backgroundColor: '#f8d7da',
        borderColor: '#f5c6cb',
      };
    default:
      return {
        color: '#1b1e21',
        backgroundColor: '#d6d8d9',
        borderColor: '#c6c8ca',
      };
  }
}
