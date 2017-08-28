import * as React from 'react';
import {NodeInfoMap} from '../immutable';
import {NodeState} from '../NodeInfo';
import {NodeCard} from './';

interface ListViewOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

interface ListViewProps {
  nodeInfoMap: NodeInfoMap;
  selection: Array<string>;
  changeSelection: (ids: Array<string>) => void;
}

interface ListViewState {
  labelFilterRegexp: RegExp;
}

const DEFAULT_REGEX = new RegExp('');

export class ListView extends React.Component<ListViewProps & ListViewOwnProps, ListViewState> {

  constructor(props: ListViewProps) {
    super(props);
    this.state = {
      labelFilterRegexp: DEFAULT_REGEX,
    };
  }

  private handleFilterChange = (event: React.FormEvent<HTMLInputElement>) => {
    this.setState({
      labelFilterRegexp: new RegExp(event.currentTarget.value, 'i')
    });
  };

  private filterPredicate = (entry: [string, NodeState]): boolean => {
    return entry[1].label.search(this.state.labelFilterRegexp) >= 0;
  };

  private static compareByLabel(a: [string, NodeState], b: [string, NodeState]) {
    return a[1].label.localeCompare(b[1].label);
  }

  private static isNodeSelected(id: string, selection: Array<string>): boolean {
    return selection.indexOf(id) >= 0;
  }

  render() {
    const {className, style, nodeInfoMap, changeSelection, selection} = this.props;

    return (<div className={className} style={style}>
      <label className='filter'>Filter
        <input type='search' autoComplete='on' autoFocus={true} placeholder='enter substring'
               onChange={this.handleFilterChange}/>
      </label>
      <ul className='itemlist'>{
        nodeInfoMap.entrySeq()
          .filter(this.filterPredicate)
          .sort(ListView.compareByLabel)
          .map(o => o && <li key={o[0]} onClick={() => changeSelection([o[0]])}>
            <NodeCard id={o[0]} isSelected={ListView.isNodeSelected(o[0], selection)} nodeState={o[1]}/>
          </li>)
      }</ul>
    </div>);
  }
}
