import * as Immutable from 'immutable';
import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';
import {HealthStatus} from '../WebSocketJson';

export interface NodeInfo {
  readonly id: string;
  readonly serviceName: string;
  readonly host: string;
  readonly lastUpdatedInstant: number;
  readonly healthStatus: HealthStatus;
  readonly lastPollResult?: string;
  readonly depends?: Immutable.Set<string>;
  readonly lastPollInstant?: number;
  readonly lastPollDurationMillis?: number;
}

export interface NodeInfoRecord extends TypedRecord<NodeInfoRecord>, NodeInfo {
}

const defaultNodeInfo = {
  id: '',
  serviceName: '',
  host: '',
  lastUpdatedInstant: 0,
  healthStatus: HealthStatus.Unknown,
  lastPollResult: undefined,
  depends: undefined,
  lastPollInstant: undefined,
  lastPollDurationMillis: undefined,
};

export const NodeInfoRecordFactory = makeTypedFactory<NodeInfo, NodeInfoRecord>(defaultNodeInfo);

export interface ServiceInstances {
  readonly [id: string]: NodeInfo;
}

export type InstancesRecordMap = Immutable.Map<string, NodeInfoRecord>;

export const EMPTY_INSTANCES_RECORD_MAP = Immutable.Map<string, NodeInfoRecord>();
