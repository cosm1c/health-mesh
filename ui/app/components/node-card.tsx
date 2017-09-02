import * as React from 'react';
import {colorForHealth} from '../NodeInfo';
import {NodeInfoRecord} from '../immutable/NodeInfoRecord';

export interface NodeCardOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

export interface NodeCardProps {
  nodeInfoRecord: NodeInfoRecord;
  isSelected?: boolean;
  recentlyUpdate?: boolean;
}

export const NodeCard: React.StatelessComponent<NodeCardOwnProps & NodeCardProps> = (props) => {
  const {nodeInfoRecord, isSelected, className, style} = props;
  const {color, backgroundColor, borderColor} = colorForHealth(nodeInfoRecord.healthStatus);
  // TODO: flash card when recentlyUpdated=true
  const nodeStyle = {
    ...style,
    color: color,
    backgroundColor: isSelected ? 'white' : backgroundColor,
    borderColor: borderColor,
  };

  return (<div className={className} style={nodeStyle}>{nodeInfoRecord.label}</div>);
};
