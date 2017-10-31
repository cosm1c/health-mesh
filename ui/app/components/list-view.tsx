import * as React from 'react';
import {ServiceInfoRecord, ServicesRecordMap} from '../immutable';
import {FormControl, FormGroup, InputGroup, ListGroup, ListGroupItem} from 'react-bootstrap';
import {bsStyleForHealth} from '../WebSocketJson';

interface ListViewOwnProps {
  className?: string;
  style?: React.CSSProperties;
}

interface ListViewProps {
  nodeInfoRecordMap: ServicesRecordMap;
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

  private handleFilterChange = (event: React.FormEvent<FormControl>) => {
    this.setState({
      labelFilterRegexp: new RegExp((event.currentTarget as any).value as string, 'i')
    });
  };

  private filterPredicate = (entry: ServiceInfoRecord): boolean => {
    return entry.serviceName.search(this.state.labelFilterRegexp) >= 0;
  };

  private static compareByLabel(a: ServiceInfoRecord, b: ServiceInfoRecord) {
    return a.serviceName.localeCompare(b.serviceName);
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
          .map(o => o &&
            <ListGroupItem key={o.serviceName} bsStyle={bsStyleForHealth(o.healthStatus)}
                           active={ListView.isNodeSelected(o.serviceName, selection)}
                           onClick={() => changeSelection([o.serviceName])}>{o.serviceName}</ListGroupItem>)
      }</ListGroup>
    </div>);
  }
}
