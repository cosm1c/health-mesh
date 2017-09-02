import {Reducer} from 'redux';
import {INDEX_UPDATED, MetadataAction, USER_COUNT_UPDATED} from './actions';
import {IMetadataStateRecord, MetadataStateFactory} from '../../immutable/MetadataStateRecord';

export const initialMetadataState = MetadataStateFactory();

export const metadataReducer: Reducer<IMetadataStateRecord> =
  (state: IMetadataStateRecord = initialMetadataState, action: MetadataAction): IMetadataStateRecord => {
    switch (action.type) {
      case INDEX_UPDATED:
        return state.setIn(['index'], action.index);

      case USER_COUNT_UPDATED:
        return state.setIn(['userCount'], action.count);

      default:
        return state;
    }
  };
