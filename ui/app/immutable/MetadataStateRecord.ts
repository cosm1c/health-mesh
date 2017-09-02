import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';
import {EMPTY_NODE_INFO_RECORD_MAP, NodeInfoRecordMap} from './NodeInfoRecord';

interface IMetadataState {
  readonly index: NodeInfoRecordMap;
  readonly userCount: number;
}

export interface IMetadataStateRecord extends TypedRecord<IMetadataStateRecord>, IMetadataState {
}

const defaultMetadata = {
  index: EMPTY_NODE_INFO_RECORD_MAP,
  userCount: 0,
};

export const MetadataStateFactory = makeTypedFactory<IMetadataState, IMetadataStateRecord>(defaultMetadata);
