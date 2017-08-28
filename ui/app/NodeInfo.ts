export interface NodeState {
  readonly label: string;
  readonly depends: Array<string>;
  readonly cssHexColor: string;
}

export interface NodeCollectionJson {
  [id: string]: NodeState;
}

export interface NodeDeltasJson {
  readonly added: NodeCollectionJson;
  readonly updated: NodeCollectionJson;
  readonly removed: Array<string>;
}
