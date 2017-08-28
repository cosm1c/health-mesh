import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';

export enum WebSocketStateEnum {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  DISCONNECTING,
}

export function websocketStateDisplay(webSocketStateEnum: WebSocketStateEnum) {
  return WebSocketStateEnum[webSocketStateEnum];
}

interface IWebSocketState {
  socketState: WebSocketStateEnum;
}

const defaultWebSocketState: IWebSocketState = {
  socketState: WebSocketStateEnum.DISCONNECTED
};

export interface IWebSocketStateRecord extends TypedRecord<IWebSocketStateRecord>, IWebSocketState {
}

export const WebSocketStateRecordFactory = makeTypedFactory<IWebSocketState, IWebSocketStateRecord>(defaultWebSocketState);
