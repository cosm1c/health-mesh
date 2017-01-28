import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/observable/dom/WebSocketSubject';
import {Delta} from './NodeInfo';

require('../less/main.less');

// Used by DefinePlugin
declare const ENV: string;

/*
function getElementByIdOrThrowError(elementId: string): HTMLElement {
  const el = document.getElementById(elementId);
  if (!el) {
    throw new Error(`[DOM] Failed to locate HTMLElement with id ${elementId}`);
  }
  return el;
}

const digraphEl = getElementByIdOrThrowError('digraph');
*/

function calcWsUrl(): string {
  if (ENV === 'development') {
    const wsUrl = 'ws://localhost:8080/ws';
    const msg = 'DEVELOPMENT MODE ENGAGED - websocket URL:';
    // '='.repeat(msg.length + wsUrl.length + 1) +
    console.warn(`======================================\n${msg}`, wsUrl);
    return wsUrl;
  }

  if (window.location.protocol !== 'https:') {
    console.warn('Using insecure protocol', window.location.protocol);
  }
  return `${(window.location.protocol === 'https:') ? 'wss://' : 'ws://'}${window.location.hostname}:${window.location.port}/ws`;
}


const webSocketSubjectConfig: WebSocketSubjectConfig = {
  url: calcWsUrl(),
  openObserver: {
    next: (value: Event) => {
      console.log('WebSocket Opened', value);
    }
  },
  closeObserver: {
    next: (closeEvent: CloseEvent) => {
      console.log('WebSocket Close', closeEvent);
    }
  },
  closingObserver: {
    next: (voidValue: void) => {
      console.log('WebSocket Closing', voidValue);
    }
  }
};


window.onload = () => {
  console.info('onload');
  const socket: WebSocketSubject<Delta> = WebSocketSubject.create(webSocketSubjectConfig);

  socket.subscribe(
    // next
    function (delta: Delta) {
      // console.debug('Delta', delta);
      const addNodes = Object.keys(delta.add).map((id) => delta.add[id]);
      const delNodes = Object.keys(delta.del).map((id) => delta.del[id]);
      console.debug('add:', addNodes, ' del:', delNodes);
    },
    // error
    function (error: any) {
      // errors and "unclean" closes land here
      console.error('error:', error);
    },
    // complete
    function () {
      // the socket has been closed
      console.info('socket closed');
    }
  );
};

