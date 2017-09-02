import * as React from 'react';
import * as Immutable from 'immutable';
import {NodeCard} from './';
import {NodeInfoRecord, NodeInfoRecordMap} from '../immutable';

interface ListViewOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

interface ListViewProps {
  nodeInfoRecordMap: NodeInfoRecordMap;
  selection: Array<string>;
  changeSelection: (ids: Array<string>) => void;
  recentlyUpdated: Immutable.Set<string>;
}

interface ListViewState {
  labelFilterRegexp: RegExp;
}

const DEFAULT_REGEX = new RegExp('');

type NodeCardListItemProps = {
  onClick: React.MouseEventHandler<any>;
  isSelected: boolean;
  nodeInfoRecord: NodeInfoRecord;
  recentlyUpdate: boolean;
};

const NodeCardListItem: React.StatelessComponent<NodeCardListItemProps> = (props) => {
  const {onClick, nodeInfoRecord, isSelected, recentlyUpdate} = props;

  return (<li key={nodeInfoRecord.id} onClick={onClick}>
    <NodeCard isSelected={isSelected} nodeInfoRecord={nodeInfoRecord} recentlyUpdate={recentlyUpdate}/>
  </li>);
};


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

  private filterPredicate = (entry: NodeInfoRecord): boolean => {
    return entry.label.search(this.state.labelFilterRegexp) >= 0;
  };

  private static compareByLabel(a: NodeInfoRecord, b: NodeInfoRecord) {
    return a.label.localeCompare(b.label);
  }

  private static isNodeSelected(id: string, selection: Array<string>): boolean {
    return selection.indexOf(id) >= 0;
  }

  render() {
    const {className, style, nodeInfoRecordMap, changeSelection, selection} = this.props;

    return (<div className={className} style={style}>
      <label className='filter'>Filter
        <input type='search' autoComplete='on' autoFocus={true} placeholder='enter substring'
               onChange={this.handleFilterChange}/>
      </label>
      <ul className='itemlist'>{
        nodeInfoRecordMap.valueSeq()
          .filter(this.filterPredicate)
          .sort(ListView.compareByLabel)
          .map(o =>
            <NodeCardListItem key={o!.id} nodeInfoRecord={o!} isSelected={ListView.isNodeSelected(o!.id, selection)}
                              recentlyUpdate={this.props.recentlyUpdated.has(o!.id)}
                              onClick={() => changeSelection([o!.id])}/>)
      }</ul>
    </div>);
  }
}
