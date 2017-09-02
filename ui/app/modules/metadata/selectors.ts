import {createSelector, OutputSelector} from 'reselect';
import {IRootStateRecord} from '../root-reducer';
import {IMetadataStateRecord, NodeInfoRecordMap} from '../../immutable';

export const getMetadataIndex: OutputSelector<IRootStateRecord, NodeInfoRecordMap, (res: IMetadataStateRecord) => NodeInfoRecordMap> =
  createSelector(
    (rootState: IRootStateRecord) => rootState.get('metadataState'),
    (state) => state.get('index')
  );

export const getUserCount: OutputSelector<IRootStateRecord, number, (res: IMetadataStateRecord) => number> =
  createSelector(
    (rootState: IRootStateRecord) => rootState.get('metadataState'),
    (state) => state.get('userCount')
  );
