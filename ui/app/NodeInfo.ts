export interface NodeInfo {
  label: string;
  depends: Array<String>;
  cssHexColor: string;
}

export interface DeltaItem {
  [id: string]: NodeInfo;
}

export interface Delta {
  added: DeltaItem;
  updated: DeltaItem;
  removed: Array<string>;
}
