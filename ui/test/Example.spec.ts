/// <reference path="../../typings/index.d.ts" />
import * as Chai from 'chai';
import {ExampleMessageCodec} from '../../app/Example';
const assert = Chai.assert;

describe('RingBuffer', () => {

  describe('forEach', () => {
    it('reads JSON', () => {
      let exampleModel = ExampleMessageCodec.readText('{"message":"helloWorld"}');

      assert.deepEqual(exampleModel, {message: 'helloWorld'});
    });
  });

});
