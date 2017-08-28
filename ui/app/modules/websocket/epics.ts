import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/observable/dom/WebSocketSubject';
import 'rxjs/add/observable/timer';
import 'rxjs/add/observable/fromEvent';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/retryWhen';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/take';
import 'rxjs/add/operator/takeUntil';
import {combineEpics, Epic} from 'redux-observable';
import {actionCreators, CONNECT_WEBSOCKET, DISCONNECT_WEBSOCKET} from './';
import {IRootAction, IRootStateRecord} from '../../modules';
import {NodeDeltasJson} from '../../NodeInfo';
import store from '../../store';
import {WebSocketAction} from './actions';
import {epic$} from '../root-epic';

// Used by DefinePlugin
declare const IS_PROD: string;
// TODO: use exponential backoff - eg: Math.min(30, (Math.pow(2, k) - 1)) * 1000
const RECONNECT_DELAY_MS = 1000;

function calcWsUrl(): Promise<string> {
  if (!IS_PROD) {
    const wsUrl = 'ws://localhost:18080/health-mesh/ws';
    const msg = `[${new Date().toISOString()}] DEVELOPMENT MODE ENGAGED - websocket URL:`;
    // '='.repeat(msg.length + wsUrl.length + 1) +
    console.warn(`======================================\n${msg}`, wsUrl);
    return Promise.resolve(wsUrl);
  }

  if (window.location.protocol !== 'https:') {
    console.warn('Using insecure ws protocol as page loaded with', window.location.protocol);
  }

  const httpPathPrefix = window.location.pathname.substring(1, window.location.pathname.indexOf('/', 1));
  const fetchWsURl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/${httpPathPrefix}/wsUrl`;
  return new Promise((resolve, reject) => {
    console.debug('Fetching WebSocket URL from', fetchWsURl);
    try {
      const xhr = new XMLHttpRequest();
      xhr.open('get', fetchWsURl, true);
      xhr.responseType = 'json';
      xhr.onload = () => {
        const status = xhr.status;
        if (status === 200) {
          console.info('Using wsUrl:', xhr.response.wsUrl);
          resolve(xhr.response.wsUrl);
        } else {
          reject(`Failed to fetch wsUrl from ${fetchWsURl} - http response status ${status}`);
        }
      };
      xhr.send();
    } catch (e) {
      reject(`Failed to fetch wsUrl from ${fetchWsURl} - error: ${e}`);
    }
  });
}

const eventualSocket: Promise<WebSocketSubject<NodeDeltasJson>> =
  calcWsUrl()
    .then(wsUrl => {
      console.info('Using WebSocket URL', wsUrl);
      const webSocketSubjectConfig: WebSocketSubjectConfig = {
        url: wsUrl,
        openObserver: {
          next: () => {
            console.info(`[${new Date().toISOString()}] WebSocket connected`);
            const action = actionCreators.websocketConnected();
            webSocketActionSubject.next(action);
            return store.dispatch(action);
          }
        },
        closeObserver: {
          next: () => {
            console.info(`[${new Date().toISOString()}] WebSocket disconnected`);
            const action = actionCreators.websocketDisconnected();
            webSocketActionSubject.next(action);
            return store.dispatch(action);
          }
        },
      };

      // TODO: for testing use store dependencies to inject websocket creator
      return WebSocketSubject.create(webSocketSubjectConfig) as WebSocketSubject<NodeDeltasJson>;
    });

// add epic asynchronously
eventualSocket.then(socket => {
  const websocketEpic: Epic<IRootAction, IRootStateRecord, {}> = action$ =>
    action$
      .ofType(CONNECT_WEBSOCKET)
      .switchMap(() =>
        socket
          .retryWhen(errors => errors.mergeMap(() => {
            if (window.navigator.onLine) {
              console.debug(`Reconnecting WebSocket in ${RECONNECT_DELAY_MS}ms`);
              return Observable.timer(RECONNECT_DELAY_MS);
            }
            return Observable.fromEvent(window, 'online')
              .take(1);
          }))
          .takeUntil(
            action$.ofType(DISCONNECT_WEBSOCKET)
          )
          .map(actionCreators.websocketPayload)
          .map(action => {
            webSocketActionSubject.next(action);
            return action;
          })
      );

  epic$.next(websocketEpic);

  store.dispatch(actionCreators.connectWebsocket());
});

export const epics = combineEpics();

export const webSocketActionSubject: Subject<WebSocketAction> = new Subject();
