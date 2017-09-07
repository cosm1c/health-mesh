import * as React from 'react';
import {websocketStateDisplay, WebSocketStateEnum} from '../immutable/WebSocketStateRecord';

export interface TaskBarOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

export interface TaskBarProps {
  edgeCount: number;
  nodeCount: number;
  socketState: WebSocketStateEnum;
  userCount: number;
}

export const StatusBar: React.StatelessComponent<TaskBarOwnProps & TaskBarProps> = (props) => {
  const {edgeCount, nodeCount, socketState, userCount, className, style} = props;

  return (<div className={className} style={style}>
    <ul>
      <li>Nodes: {nodeCount}</li>
      <li>Edges: {edgeCount}</li>
      <li>WebSocket: {websocketStateDisplay(socketState)}</li>
      <li>User Count: {userCount}</li>
    </ul>
  </div>);
};
