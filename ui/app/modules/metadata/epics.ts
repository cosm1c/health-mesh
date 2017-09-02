import {combineEpics, Epic} from 'redux-observable';
import * as Immutable from 'immutable';
import {getMetadataIndex} from './selectors';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/zip';
import {IRootAction, IRootStateRecord} from '../';
import {actionCreators} from './actions';
import {NodeInfoRecordFactory, NodeInfoRecordMap} from '../../immutable';
import {DELTA_PAYLOAD, USER_COUNT_PAYLOAD, WebSocketActions} from '../websocket/actions';
import {NodeCollectionJson, NodeDeltasJson} from '../../NodeInfo';

function upsertAll(collection: NodeCollectionJson, mutable: NodeInfoRecordMap) {
  const now = Date.now();
  for (let key in collection) {
    const updateDelta = collection[key];
    const nodeInfoRecord = mutable.get(key);

    if (nodeInfoRecord) {
      mutable.set(key,
        nodeInfoRecord
          .mergeDeep(updateDelta)
          .set('lastUpdatedInstant', now)
      );

    } else {
      mutable.set(key,
        NodeInfoRecordFactory({
          ...updateDelta,
          id: key,
          lastUpdatedInstant: now,
          label: updateDelta.label,
          depends: Immutable.List(updateDelta.depends),
          healthStatus: updateDelta.healthStatus,
          lastPollInstant: updateDelta.lastPollInstant,
          lastPollResult: updateDelta.lastPollResult,
          lastPollDurationMillis: updateDelta.lastPollDurationMillis,
        }));
    }
  }
}

function deleteAll(keys: Array<string>, mutable: NodeInfoRecordMap) {
  keys.forEach((key) => mutable.remove(key));
}

function applyMutations(deltasJson: NodeDeltasJson, index: NodeInfoRecordMap): NodeInfoRecordMap {
  return index.withMutations(mutable => {
    deleteAll(deltasJson.removed, mutable);
    upsertAll(deltasJson.updated, mutable);
    upsertAll(deltasJson.added, mutable);
  });
}

const deltaReceivedEpic: Epic<IRootAction, IRootStateRecord> = (action$, store) => {
  return action$
    .ofType(DELTA_PAYLOAD)
    .map((payloadAction: WebSocketActions[typeof DELTA_PAYLOAD]) => payloadAction.payload)
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
  deltaReceivedEpic,
  userCountReceivedEpic,
);
