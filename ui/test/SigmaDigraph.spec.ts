/// <reference path="../typings/index.d.ts" />
import {assert} from 'chai';
import {SigmaDigraph} from '../app/sigma';

describe('SigmaDigraph', () => {

  it('calculates correct node color', () => {
    assert(SigmaDigraph.colorForNode({
        id: 'id',
        lastUpdate: new Date(),
        depends: [],
        healthStatus: 'healthy'
      }) === '#00FF00');
  });
});
