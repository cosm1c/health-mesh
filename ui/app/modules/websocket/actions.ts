import {NodeDeltasJson} from '../../NodeInfo';

export const CONNECT_WEBSOCKET = 'CONNECT_WEBSOCKET';
export const DISCONNECT_WEBSOCKET = 'DISCONNECT_WEBSOCKET';
export const WEBSOCKET_CONNECTED = 'WEBSOCKET_CONNECTED';
export const WEBSOCKET_PAYLOAD = 'WEBSOCKET_PAYLOAD';
export const WEBSOCKET_DISCONNECTED = 'WEBSOCKET_DISCONNECTED';

export type WebSocketActions = {
  CONNECT_WEBSOCKET: { type: typeof CONNECT_WEBSOCKET, date: Date },
  DISCONNECT_WEBSOCKET: { type: typeof DISCONNECT_WEBSOCKET, date: Date },
  WEBSOCKET_CONNECTED: { type: typeof WEBSOCKET_CONNECTED, date: Date },
  WEBSOCKET_PAYLOAD: { type: typeof WEBSOCKET_PAYLOAD, payload: NodeDeltasJson },
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
  websocketPayload: (payload: NodeDeltasJson) => ({
    type: WEBSOCKET_PAYLOAD as typeof WEBSOCKET_PAYLOAD,
    payload,
  }),
  websocketDisconnected: () => ({
    type: WEBSOCKET_DISCONNECTED as typeof WEBSOCKET_DISCONNECTED,
    date: new Date(),
  }),
};
