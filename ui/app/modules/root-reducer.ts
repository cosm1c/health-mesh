import {combineReducers} from 'redux-immutable';
import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';
import {initialWebSocketState, websocketReducer as websocketReducer} from './websocket';
import {IMetadataStateRecord, IWebSocketStateRecord} from '../immutable';
import {initialMetadataState} from './metadata';
import {metadataReducer} from './metadata/reducer';

interface IRootState {
  websocketState: IWebSocketStateRecord;
  metadataState: IMetadataStateRecord;
}

const defaultRootState: IRootState = {
  websocketState: initialWebSocketState,
  metadataState: initialMetadataState
};

export interface IRootStateRecord extends TypedRecord<IRootStateRecord>, IRootState {
}

const InitialStateFactory = makeTypedFactory<IRootState, IRootStateRecord>(defaultRootState);

export const initialState = InitialStateFactory(defaultRootState);

export const rootReducer = combineReducers<IRootStateRecord>(
  {
    websocketState: websocketReducer,
    metadataState: metadataReducer,
  }
  // do we need to provide getDefaultState here?
);
