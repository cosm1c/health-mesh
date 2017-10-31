import * as Immutable from 'immutable';
import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';
import {HealthStatus} from '../WebSocketJson';
import {EMPTY_INSTANCES_RECORD_MAP, InstancesRecordMap} from './NodeInfoRecord';

interface ServiceInfo {
  readonly serviceName: string;
  readonly total: number;
  readonly alerts: number;
  readonly healthStatus: HealthStatus;
  readonly depends: Immutable.Set<string>;
  readonly instances: InstancesRecordMap;
}

export interface ServiceInfoRecord extends TypedRecord<ServiceInfoRecord>, ServiceInfo {
}

const defaultServiceInfo = {
  serviceName: '',
  total: 0,
  alerts: 0,
  healthStatus: HealthStatus.Unknown,
  depends: Immutable.Set<string>(),
  instances: EMPTY_INSTANCES_RECORD_MAP,
};

export const ServiceInfoRecordFactory = makeTypedFactory<ServiceInfo, ServiceInfoRecord>(defaultServiceInfo);

export type ServicesRecordMap = Immutable.Map<string, ServiceInfoRecord>;

export const EMPTY_SERVICES_RECORD_MAP = Immutable.Map<string, ServiceInfoRecord>();
