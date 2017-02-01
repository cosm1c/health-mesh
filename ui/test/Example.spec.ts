/// <reference path="../typings/index.d.ts" />
import * as Chai from 'chai';
const assert = Chai.assert;

describe('RingBuffer', () => {

  it('Dummy Test', () => {
    let exampleModel = JSON.parse('{"message":"helloWorld"}');

    assert.deepEqual(exampleModel, {message: 'helloWorld'});
  });
});
