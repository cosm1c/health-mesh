import {NodeInfoMap} from '../../immutable';

export const INDEX_UPDATED = 'INDEX_UPDATED';

export type MetadataActions = {
  INDEX_UPDATED: { type: typeof INDEX_UPDATED, index: NodeInfoMap; },
};

export type MetadataAction = MetadataActions[keyof MetadataActions];

export const actionCreators = {
  indexUpdated: (index: NodeInfoMap) => ({
    type: INDEX_UPDATED as typeof INDEX_UPDATED,
    index,
  }),
};
