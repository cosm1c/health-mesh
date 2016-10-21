const proto = require("../generated/ExampleModel_pb.js") as any;

export interface ExampleModel {
  message: string;
}

export class ExampleMessageCodec {

  static resultSelector(event: MessageEvent): ExampleModel {
    if (event.data instanceof ArrayBuffer) {
      return ExampleMessageCodec.readBinary(event.data);
    }
    return ExampleMessageCodec.readText(event.data);
  }

  static readBinary(data: ArrayBuffer): ExampleModel {
    return proto.ExampleModel
      .deserializeBinary(data)
      .toObject();
  }

  static writeBinary(entity: ExampleModel): ArrayBuffer|Blob {
    let data = new proto.ExampleModel();
    data.setMessage(entity.message);
    return data.serializeBinary();
  }

  static readText(text: string): ExampleModel {
    return JSON.parse(text);
  }

  static writeText(entity: ExampleModel): string {
    return JSON.stringify(entity);
  }
}
