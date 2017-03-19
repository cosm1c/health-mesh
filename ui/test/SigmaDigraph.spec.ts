/// <reference path="../typings/index.d.ts" />
import {SigmaDigraph} from '../app/sigma';

describe('SigmaDigraph', () => {

  it('calculates correct node color', () => {
    expect(SigmaDigraph.colorForNode({
      id: 'id',
      label: 'idLabel',
      depends: [],
      healthStatus: 'healthy'
    })).toBe('#00FF00');
  });
});
