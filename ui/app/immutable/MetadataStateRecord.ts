import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';
import {EMPTY_NODE_INFO_MAP, NodeInfoMap} from './NodeInfoRecord';

interface IMetadataState {
  readonly index: NodeInfoMap;
}

export interface IMetadataStateRecord extends TypedRecord<IMetadataStateRecord>, IMetadataState {
}

const defaultMetadata = {
  index: EMPTY_NODE_INFO_MAP
};

export const MetadataStateFactory = makeTypedFactory<IMetadataState, IMetadataStateRecord>(defaultMetadata);
