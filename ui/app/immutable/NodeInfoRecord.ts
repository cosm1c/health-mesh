import * as Immutable from 'immutable';
import {makeTypedFactory, TypedRecord} from 'typed-immutable-record';

interface NodeInfo {
  readonly id: string;
  readonly lastUpdatedInstant: number;
  readonly label: string;
  readonly depends: Immutable.List<string>;
  readonly healthStatus: string;
  readonly lastPollInstant?: number;
  readonly lastPollResult?: string;
  readonly lastPollDurationMillis?: number;
}

export interface NodeInfoRecord extends TypedRecord<NodeInfoRecord>, NodeInfo {
}

const defaultNodeInfo = {
  id: '',
  lastUpdatedInstant: 0,
  label: '',
  depends: Immutable.List<string>(),
  healthStatus: '',
  lastPollInstant: undefined,
  lastPollResult: undefined,
  lastPollDurationMillis: undefined,
};

export const NodeInfoRecordFactory = makeTypedFactory<NodeInfo, NodeInfoRecord>(defaultNodeInfo);

export type NodeInfoRecordMap = Immutable.Map<string, NodeInfoRecord>;

export const EMPTY_NODE_INFO_RECORD_MAP = Immutable.Map<string, NodeInfoRecord>();
