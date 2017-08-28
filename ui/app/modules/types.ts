import {Dispatch as ReduxDispatch} from 'redux';
import {IRootStateRecord} from '../modules';

export type Dispatch = ReduxDispatch<IRootStateRecord>;
