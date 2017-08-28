import {createSelector, OutputSelector} from 'reselect';
import {IRootStateRecord} from '../root-reducer';
import {IWebSocketStateRecord, WebSocketStateEnum} from '../../immutable';

export const getWebSocketStateEnum: OutputSelector<IRootStateRecord, WebSocketStateEnum, (res: IWebSocketStateRecord) => WebSocketStateEnum> =
  createSelector(
    (rootState: IRootStateRecord) => rootState.get('websocketState'),
    (websocketState) => websocketState.get('socketState')
  );
