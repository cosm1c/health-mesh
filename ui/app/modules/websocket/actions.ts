import {NodeDeltasJson} from '../../NodeInfo';

export const CONNECT_WEBSOCKET = 'CONNECT_WEBSOCKET';
export const DISCONNECT_WEBSOCKET = 'DISCONNECT_WEBSOCKET';
export const WEBSOCKET_CONNECTED = 'WEBSOCKET_CONNECTED';
export const DELTA_PAYLOAD = 'DELTA_PAYLOAD';
export const USER_COUNT_PAYLOAD = 'USER_COUNT_PAYLOAD';
export const WEBSOCKET_DISCONNECTED = 'WEBSOCKET_DISCONNECTED';

export type WebSocketActions = {
  CONNECT_WEBSOCKET: { type: typeof CONNECT_WEBSOCKET, date: Date },
  DISCONNECT_WEBSOCKET: { type: typeof DISCONNECT_WEBSOCKET, date: Date },
  WEBSOCKET_CONNECTED: { type: typeof WEBSOCKET_CONNECTED, date: Date },
  DELTA_PAYLOAD: { type: typeof DELTA_PAYLOAD, payload: NodeDeltasJson },
  USER_COUNT_PAYLOAD: { type: typeof USER_COUNT_PAYLOAD, count: number },
  WEBSOCKET_DISCONNECTED: { type: typeof WEBSOCKET_DISCONNECTED, date: Date },
};

export type WebSocketAction = WebSocketActions[keyof WebSocketActions];

export const actionCreators = {
  connectWebsocket: () => ({
    type: CONNECT_WEBSOCKET as typeof CONNECT_WEBSOCKET,
    date: new Date(),
  }),
  disconnectWebsocket: () => ({
    type: DISCONNECT_WEBSOCKET as typeof DISCONNECT_WEBSOCKET,
    date: new Date(),
  }),
  websocketConnected: () => ({
    type: WEBSOCKET_CONNECTED as typeof WEBSOCKET_CONNECTED,
    date: new Date(),
  }),
  websocketDisconnected: () => ({
    type: WEBSOCKET_DISCONNECTED as typeof WEBSOCKET_DISCONNECTED,
    date: new Date(),
  }),
  deltaPayload: (payload: NodeDeltasJson) => ({
    type: DELTA_PAYLOAD as typeof DELTA_PAYLOAD,
    payload,
  }),
  userCountPayload: (count: number) => ({
    type: USER_COUNT_PAYLOAD as typeof USER_COUNT_PAYLOAD,
    count,
  }),
};
