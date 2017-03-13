export interface NodeInfo {
  id: string;
  label: string;
  healthStatus: 'unknown' | 'healthy' | 'unhealthy';
  depends: Array<String>;
}

export interface DeltaItem {
  [id: string]: NodeInfo;
}

export interface Delta {
  add: DeltaItem;
  del: DeltaItem;
}
