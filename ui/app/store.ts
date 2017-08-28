import {applyMiddleware, compose, createStore} from 'redux';
import {createEpicMiddleware} from 'redux-observable';
import {initialState, IRootStateRecord, rootEpic, rootReducer} from './modules';

const composeEnhancers = (
  process.env.NODE_ENV === 'development' && window && window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__
) || compose;

function configureStore(initialState?: IRootStateRecord) {

  const middlewares = [
    createEpicMiddleware(rootEpic),
  ];

  const enhancer = composeEnhancers(
    applyMiddleware(...middlewares),
  );

  return createStore<IRootStateRecord>(
    rootReducer,
    initialState!,
    enhancer,
  );
}

const store = configureStore(initialState);

export default store;
