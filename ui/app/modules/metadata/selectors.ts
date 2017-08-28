import {createSelector, OutputSelector} from 'reselect';
import {IRootStateRecord} from '../root-reducer';
import {IMetadataStateRecord, NodeInfoMap} from '../../immutable';

export const getMetadataIndex: OutputSelector<IRootStateRecord, NodeInfoMap, (res: IMetadataStateRecord) => NodeInfoMap> =
  createSelector(
    (rootState: IRootStateRecord) => rootState.get('metadataState'),
    (state) => state.get('index')
  );
