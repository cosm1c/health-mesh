import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/observable/dom/WebSocketSubject';
import {ExampleMessageCodec, ExampleModel} from './Example';

const webSocketSubjectConfig: WebSocketSubjectConfig = {
  url: 'ws://localhost:8080/ws',
  resultSelector: ExampleMessageCodec.resultSelector,
  openObserver: {
    next: (value: Event) => {
      console.log('WebSocket Opened', value);
      socket.socket.binaryType = 'arraybuffer';
      socket.socket.send(ExampleMessageCodec.writeText({message: 'Text from Browser'}));
      socket.socket.send(ExampleMessageCodec.writeBinary({message: 'Binary from Browser'}));
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

const socket: WebSocketSubject<ExampleModel> = WebSocketSubject.create(webSocketSubjectConfig);

socket.subscribe(
  // next
  function (e) {
    console.debug('message', e);
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
