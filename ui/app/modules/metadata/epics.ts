import {combineEpics, Epic} from 'redux-observable';
import {List} from 'immutable';
import {getMetadataIndex} from './selectors';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/zip';
import {IRootAction, IRootStateRecord} from '../';
import {actionCreators} from './actions';
import {NodeInfoFactory, NodeInfoMap} from '../../immutable';
import {WEBSOCKET_PAYLOAD, WebSocketActions} from '../websocket/actions';
import {NodeCollectionJson, NodeDeltasJson} from '../../NodeInfo';

function upsertAll(collection: NodeCollectionJson, mutable: NodeInfoMap) {
  for (let key in collection) {
    const updateDelta = collection[key];
    const nodeInfoRecord = mutable.get(key);

    if (nodeInfoRecord) {
      mutable.set(key, nodeInfoRecord.mergeDeep(updateDelta));

    } else {
      mutable.set(key, NodeInfoFactory({
        ...updateDelta,
        depends: List(updateDelta.depends)
      }));
    }
  }
}

function deleteAll(keys: Array<string>, mutatedIndex: NodeInfoMap) {
  keys.forEach((key) => mutatedIndex.remove(key));
}

function applyMutations(deltasJson: NodeDeltasJson, index: NodeInfoMap): NodeInfoMap {
  return index.withMutations(mutable => {
    deleteAll(deltasJson.removed, mutable);
    upsertAll(deltasJson.updated, mutable);
    upsertAll(deltasJson.added, mutable);
  });
}

const payloadReceivedEpic: Epic<IRootAction, IRootStateRecord> = (action$, store) => {
  return action$
    .ofType(WEBSOCKET_PAYLOAD)
    .map((payloadAction: WebSocketActions[typeof WEBSOCKET_PAYLOAD]) => payloadAction.payload)
    .map(delta => applyMutations(delta, getMetadataIndex(store.getState())))
    .map(actionCreators.indexUpdated);
};

export const epics = combineEpics(payloadReceivedEpic);
