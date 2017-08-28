import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ActionsObservable, combineEpics} from 'redux-observable';
import {epics as websocket} from './websocket/epics';
import {epics as metadata} from './metadata/epics';
import {MiddlewareAPI} from 'redux';
import {IRootAction} from './root-actions';
import {IRootStateRecord} from './root-reducer';

export const epic$ = new BehaviorSubject(combineEpics(
  websocket,
  metadata,
));

export const rootEpic = (action$: ActionsObservable<IRootAction>, store: MiddlewareAPI<IRootStateRecord>) =>
  epic$.mergeMap(epic =>
    epic(action$, store, undefined)
  );
