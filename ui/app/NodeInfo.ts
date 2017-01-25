export interface NodeInfo {
  id: string;
  healthStatus: 'unknown' | 'healthy' | 'unhealthy';
  depends: Array<String>;
  lastUpdate: Date;
}

export interface DeltaItem {
  [id: string]: NodeInfo;
}

export interface Delta {
  add: DeltaItem;
  del: DeltaItem;
}
