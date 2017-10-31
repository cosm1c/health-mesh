import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';
import {EMPTY_SERVICES_RECORD_MAP, ServicesRecordMap} from './ServiceInfoRecord';

interface IMetadataState {
  readonly index: ServicesRecordMap;
  readonly userCount: number;
}

export interface IMetadataStateRecord extends TypedRecord<IMetadataStateRecord>, IMetadataState {
}

const defaultMetadata = {
  index: EMPTY_SERVICES_RECORD_MAP,
  userCount: 0,
};

export const MetadataStateFactory = makeTypedFactory<IMetadataState, IMetadataStateRecord>(defaultMetadata);
