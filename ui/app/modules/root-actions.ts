// RootActions
import {WebSocketAction} from './websocket';
import {MetadataAction} from './metadata';

export type IRootAction =
  WebSocketAction | MetadataAction;

// import { returntypeof } from 'react-redux-typescript';

// // merging actions returned from all action creators
// const actions = Object.values({
//   ...converterActionCreators,
// }).map(returntypeof);

// export type IRootAction = typeof actions[number];
