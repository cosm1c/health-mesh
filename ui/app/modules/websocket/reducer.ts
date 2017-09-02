import {Reducer} from 'redux';
import {
  CONNECT_WEBSOCKET,
  DISCONNECT_WEBSOCKET,
  WEBSOCKET_CONNECTED,
  WEBSOCKET_DISCONNECTED,
  WebSocketAction,
} from './actions';
import {IWebSocketStateRecord, WebSocketStateEnum, WebSocketStateRecordFactory} from '../../immutable';

export const initialWebSocketState = WebSocketStateRecordFactory();

export const websocketReducer: Reducer<IWebSocketStateRecord> =
  (state: IWebSocketStateRecord = initialWebSocketState, action: WebSocketAction): IWebSocketStateRecord => {
    switch (action.type) {
      case CONNECT_WEBSOCKET:
        return state.setIn(['socketState'], WebSocketStateEnum.CONNECTING);

      case WEBSOCKET_CONNECTED:
        return state.setIn(['socketState'], WebSocketStateEnum.CONNECTED);

      case DISCONNECT_WEBSOCKET:
        return state.setIn(['socketState'], WebSocketStateEnum.DISCONNECTING);

      case WEBSOCKET_DISCONNECTED:
        return state.setIn(['socketState'], WebSocketStateEnum.DISCONNECTED);

      // case DELTA_PAYLOAD:
      //   return state.setIn(['socketState'], WebSocketStateEnum.CONNECTED);

      default:
        return state;
    }
  };
