import {ServicesRecordMap} from '../../immutable';

export const INDEX_UPDATED = 'INDEX_UPDATED';
export const USER_COUNT_UPDATED = 'USER_COUNT_UPDATED';

export type MetadataActions = {
  INDEX_UPDATED: { type: typeof INDEX_UPDATED, index: ServicesRecordMap; },
  USER_COUNT_UPDATED: { type: typeof USER_COUNT_UPDATED, count: number; },
};

export type MetadataAction = MetadataActions[keyof MetadataActions];

export const actionCreators = {
  indexUpdated: (index: ServicesRecordMap) => ({
    type: INDEX_UPDATED as typeof INDEX_UPDATED,
    index,
  }),
  userCountUpdated: (count: number) => ({
    type: USER_COUNT_UPDATED as typeof USER_COUNT_UPDATED,
    count,
  }),
};
