import * as React from 'react';
import {NodeState} from '../NodeInfo';

export interface NodeCardOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

export interface NodeCardProps {
  id: string;
  nodeState: NodeState;
  isSelected?: boolean;
}

export const NodeCard: React.StatelessComponent<NodeCardOwnProps & NodeCardProps> = (props) => {
  const {nodeState, isSelected, className, style} = props;
  const nodeStyle = {
    ...style,
    color: isSelected ? nodeState.cssHexColor : 'white',
    backgroundColor: isSelected ? 'white' : nodeState.cssHexColor,
  };

  return (<div className={className} style={nodeStyle}>{nodeState.label}</div>);
};
