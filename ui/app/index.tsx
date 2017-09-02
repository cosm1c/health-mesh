import 'es6-promise/auto';
import * as React from 'react';
import {render} from 'react-dom';
import {Provider} from 'react-redux';
import './main.less';
import store from './store';
import {Home} from './containers';

const Root = (
  <Provider store={store}>
    <Home/>
  </Provider>
);

render(Root, document.getElementById('root'));
