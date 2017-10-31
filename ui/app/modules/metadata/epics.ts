import * as Immutable from 'immutable';
import {combineEpics, Epic} from 'redux-observable';
import {getMetadataIndex} from './selectors';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/zip';
import {IRootAction, IRootStateRecord} from '../';
import {actionCreators} from './actions';
import {DELTA_PAYLOAD, USER_COUNT_PAYLOAD, WEBSOCKET_CONNECTED, WebSocketActions} from '../websocket/actions';
import {HealthStatus, NodeCollectionJson, NodeDeltasJson, NodeStateJson} from '../../WebSocketJson';
import {publishDigraphDelta} from '../digraph/DigraphSubject';
import {
  EMPTY_INSTANCES_RECORD_MAP,
  EMPTY_SERVICES_RECORD_MAP,
  NodeInfoRecord,
  NodeInfoRecordFactory,
  ServiceInfoRecord,
  ServiceInfoRecordFactory,
  ServicesRecordMap
} from '../../immutable';

const nodeIdToServiceNameMap = new Map<string, string>();

function nodeInfoFromJson(nodeStateJson: NodeStateJson, now: number) {
  return NodeInfoRecordFactory({
    id: nodeStateJson.details.id,
    serviceName: nodeStateJson.details.serviceName,
    host: nodeStateJson.details.host,
    lastUpdatedInstant: now,
    healthStatus: nodeStateJson.healthStatus,
    lastPollResult: nodeStateJson.lastPollResult,
    depends: nodeStateJson.depends ? Immutable.Set(nodeStateJson.depends) : undefined,
    lastPollInstant: nodeStateJson.lastPollInstant,
    lastPollDurationMillis: nodeStateJson.lastPollDurationMillis,
  });
}

function upsertInstances(collection: NodeCollectionJson, mutable: ServicesRecordMap): Set<string> {
  const now = Date.now();
  const upsertedServicesNames = new Set<string>();
  for (let nodeId in collection) {
    const nodeStateJson = collection[nodeId];
    const serviceName = nodeStateJson.details.serviceName;
    nodeIdToServiceNameMap.set(nodeId, serviceName);
    upsertedServicesNames.add(serviceName);

    let serviceInfoRecord: ServiceInfoRecord = mutable.get(serviceName);
    if (serviceInfoRecord) {
      const nodeInfoRecord = serviceInfoRecord.get(nodeId);
      if (nodeInfoRecord) {
        serviceInfoRecord = serviceInfoRecord.update('instances', instances =>
          instances.update(nodeId, (nodeInfo: NodeInfoRecord) =>
            nodeInfo
              .set('id', nodeId)
              .set('serviceName', serviceName)
              .set('host', nodeStateJson.details.host)
              .set('lastUpdatedInstant', now)
              .set('healthStatus', nodeStateJson.healthStatus)
              .set('lastPollResult', nodeStateJson.lastPollResult)
              .set('depends', nodeStateJson.depends ? Immutable.Set(nodeStateJson.depends) : undefined)
              .set('lastPollInstant', nodeStateJson.lastPollInstant)
              .set('lastPollDurationMillis', nodeStateJson.lastPollDurationMillis)));

      } else {
        serviceInfoRecord = serviceInfoRecord
          .update('instances', instances => instances.set(nodeId, nodeInfoFromJson(nodeStateJson, now)));
      }

    } else {
      serviceInfoRecord = ServiceInfoRecordFactory({
        serviceName: serviceName,
        total: 0,
        alerts: nodeStateJson.healthStatus === HealthStatus.Healthy ? 0 : 1,
        healthStatus: nodeStateJson.healthStatus,
        depends: nodeStateJson.depends ? Immutable.Set(nodeStateJson.depends) : Immutable.Set(),
        instances: EMPTY_INSTANCES_RECORD_MAP.set(nodeId, nodeInfoFromJson(nodeStateJson, now))
      });
    }

    mutable.set(serviceName, serviceInfoRecord);
  }
  return upsertedServicesNames;
}

function deleteAll(keys: Array<string>, mutable: ServicesRecordMap): Set<string> {
  const deletedServices = new Set<string>();
  keys.forEach(nodeId => {
    const serviceName = nodeIdToServiceNameMap.get(nodeId);
    if (serviceName) {
      nodeIdToServiceNameMap.delete(nodeId);
      const serviceInfo = mutable.get(serviceName);
      if (serviceInfo) {
        mutable.remove(serviceName);
        deletedServices.add(serviceInfo.serviceName);
      }
    }
  });
  return deletedServices;
}

function reduceHealthStatus(acc: HealthStatus, value: NodeInfoRecord): HealthStatus {
  if (acc === HealthStatus.Unhealthy || value.healthStatus === HealthStatus.Unhealthy) return HealthStatus.Unhealthy;
  if (value.healthStatus === HealthStatus.Healthy) return HealthStatus.Healthy;
  return acc;
}

function reduceDepends(set: Immutable.Set<string>, value: NodeInfoRecord): Immutable.Set<string> {
  if (!value.depends) return set;
  value.depends.forEach(i => {
    if (i) set = set.add(i);
  });
  return set;
}

function updateServiceStats(changed: Set<string>, mutable: ServicesRecordMap): Set<string> {
  const changedServices = new Set<string>();

  changed.forEach(serviceName => {
      const serviceInfo: ServiceInfoRecord = mutable.get(serviceName);
      if (serviceInfo) {
        const updatedServiceInfo = ServiceInfoRecordFactory({
          serviceName: serviceInfo.serviceName,
          total: serviceInfo.instances.size,
          alerts: serviceInfo.instances
            .map(i => !i || i.healthStatus)
            .filter(health => health !== HealthStatus.Healthy)
            .size,
          healthStatus: serviceInfo.instances.reduce(reduceHealthStatus, HealthStatus.Unknown),
          depends: serviceInfo.instances.reduce(reduceDepends, Immutable.Set<string>()),
          instances: serviceInfo.instances,
        });

        if (!updatedServiceInfo.equals(serviceInfo)) {
          changedServices.add(serviceName);
          mutable.set(serviceInfo.serviceName, serviceInfo.merge(updatedServiceInfo));
        }

      } else {
        changedServices.add(serviceName);
      }
    }
  );

  return changedServices;
}

function applyMutations(deltasJson: NodeDeltasJson, index: ServicesRecordMap): ServicesRecordMap {
  let servicesToCheck = new Set<string>();
  const updatedIndex = index.withMutations(mutable => {
    const changedServices = new Set<string>();
    let strings = deleteAll(deltasJson.removed, mutable);
    strings
      .forEach(changedServices.add, changedServices);
    upsertInstances(deltasJson.updated, mutable)
      .forEach(changedServices.add, changedServices);
    servicesToCheck = updateServiceStats(changedServices, mutable);
  });
  publishDigraphDelta(servicesToCheck, index, updatedIndex);
  return updatedIndex;
}

const websocketConnectedEpic: Epic<IRootAction, IRootStateRecord> = (action$) => {
  return action$
    .ofType(WEBSOCKET_CONNECTED)
    .map(() => actionCreators.indexUpdated(EMPTY_SERVICES_RECORD_MAP));
};

const deltaReceivedEpic: Epic<IRootAction, IRootStateRecord> = (action$, store) => {
  return action$
    .ofType(DELTA_PAYLOAD)
    .map((payloadAction: WebSocketActions[typeof DELTA_PAYLOAD]) => payloadAction.delta)
    .map(delta => applyMutations(delta, getMetadataIndex(store.getState())))
    .map(actionCreators.indexUpdated);
};

const userCountReceivedEpic: Epic<IRootAction, IRootStateRecord> = (action$) => {
  return action$
    .ofType(USER_COUNT_PAYLOAD)
    .map((payloadAction: WebSocketActions[typeof USER_COUNT_PAYLOAD]) => payloadAction.count)
    .map(actionCreators.userCountUpdated);
};

export const epics = combineEpics(
  websocketConnectedEpic,
  deltaReceivedEpic,
  userCountReceivedEpic,
);
