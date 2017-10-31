import {Color} from 'vis';

export enum HealthStatus {
  Unknown = 'Unknown',
  Healthy = 'Healthy',
  Unhealthy = 'Unhealthy'
}

export interface NodeDetails {
  readonly id: string;
  readonly serviceName: string;
  readonly host: string;
}

export interface NodeStateJson {
  readonly details: NodeDetails;
  readonly healthStatus: HealthStatus;
  readonly lastPollResult?: any;
  readonly depends?: Array<string>;
  readonly lastPollInstant?: number;
  readonly lastPollDurationMillis?: number;
}

export interface NodeCollectionJson {
  [id: string]: NodeStateJson;
}

export interface MapDeltaPacket {
  readonly delta: NodeDeltasJson;
}

export interface NodeDeltasJson {
  readonly updated: NodeCollectionJson;
  readonly removed: Array<string>;
}

export interface UserCountPacket {
  readonly userCount: number;
}

export function bsStyleForHealth(healthStatus: string): string {
  switch (healthStatus) {
    case HealthStatus.Unknown:
      return 'warning';
    case HealthStatus.Healthy:
      return 'success';
    case HealthStatus.Unhealthy:
      return 'danger';
    default:
      return 'info';
  }
}

export function graphColorForHealth(healthStatus: string): Color {
  switch (healthStatus) {
    case HealthStatus.Unknown:
      return {
        background: '#fff3cd',
        border: '#ffeeba',
      };
    case HealthStatus.Healthy:
      return {
        background: '#d4edda',
        border: '#c3e6cb',
      };
    case HealthStatus.Unhealthy:
      return {
        background: '#f8d7da',
        border: '#f5c6cb',
      };
    default:
      return {
        background: '#d6d8d9',
        border: '#c6c8ca',
      };
  }
}
