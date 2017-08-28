import {List, Map} from 'immutable';
import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';

interface INodeInfo {
  readonly label: string;
  readonly depends: List<string>;
  readonly cssHexColor: string;
}

export interface INodeInfoRecord extends TypedRecord<INodeInfoRecord>, INodeInfo {
}

const defaultNodeInfo = {
  label: '',
  depends: List<string>(),
  cssHexColor: '',
};

export const NodeInfoFactory = makeTypedFactory<INodeInfo, INodeInfoRecord>(defaultNodeInfo);

export type NodeInfoMap = Map<string, INodeInfoRecord>;

export const EMPTY_NODE_INFO_MAP = Map<string, INodeInfoRecord>();
