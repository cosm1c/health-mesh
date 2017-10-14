import * as React from 'react';
import * as Immutable from 'immutable';
import {NodeInfoRecord, NodeInfoRecordMap} from '../immutable';
import {FormControl, FormGroup, InputGroup, ListGroup, ListGroupItem} from 'react-bootstrap';
import {bsStyleForHealth} from '../NodeInfo';

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

export class ListView extends React.Component<ListViewProps & ListViewOwnProps, ListViewState> {

  constructor(props: ListViewProps) {
    super(props);
    this.state = {
      labelFilterRegexp: DEFAULT_REGEX,
    };
  }

  private handleFilterChange = (event: React.FormEvent<FormControl>) => {
    this.setState({
      labelFilterRegexp: new RegExp((event.currentTarget as any).value as string, 'i')
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
      <form>
        <FormGroup>
          <InputGroup>
            <InputGroup.Addon>Filter</InputGroup.Addon>
            <FormControl type='text' autoFocus={true} placeholder='enter substring'
                         onChange={this.handleFilterChange}/>
          </InputGroup>
        </FormGroup>
      </form>

      <ListGroup className='itemlist'>{
        nodeInfoRecordMap.valueSeq()
          .filter(this.filterPredicate)
          .sort(ListView.compareByLabel)
          .map(o =>
            <ListGroupItem key={o!.id} bsStyle={bsStyleForHealth(o!.healthStatus)}
                           active={ListView.isNodeSelected(o!.id, selection)}
                           onClick={() => changeSelection([o!.id])}>{o!.label}</ListGroupItem>)
        /*
          TODO: UX for isSelected and recentlyUpdate
          recentlyUpdate={this.props.recentlyUpdated.has(o!.id)}
        */
      }</ListGroup>
    </div>);
  }
}
