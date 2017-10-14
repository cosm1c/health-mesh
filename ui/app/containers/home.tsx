import * as React from 'react';
import {Digraph} from '../components';
import {Button, Col, Grid, Modal, Nav, Navbar, NavItem, Row} from 'react-bootstrap';

interface HomeProps {
}

interface HomeState {
  showTechStackModal: boolean;
  showAboutModal: boolean;
}

export class Home extends React.Component<HomeProps, HomeState> {

  constructor(props: HomeProps) {
    super(props);
    this.state = {
      showTechStackModal: false,
      showAboutModal: false,
    };
    this.openTechStackModal = this.openTechStackModal.bind(this);
    this.closeTechStackModal = this.closeTechStackModal.bind(this);
    this.openAboutModal = this.openAboutModal.bind(this);
    this.closeAboutModal = this.closeAboutModal.bind(this);
  }

  openTechStackModal() {
    this.setState({showTechStackModal: true});
  }

  closeTechStackModal() {
    this.setState({showTechStackModal: false});
  }

  openAboutModal() {
    this.setState({showAboutModal: true});
  }

  closeAboutModal() {
    this.setState({showAboutModal: false});
  }

  render() {
    return (<Grid>

      <Row>
        <Col>
          <Navbar>
            <Navbar.Header>
              <Navbar.Brand>Health Mesh</Navbar.Brand>
            </Navbar.Header>
            <Nav pullRight={true}>
              <NavItem eventKey={1} href='#' onClick={this.openTechStackModal}>TechStack</NavItem>
              <NavItem eventKey={2} href='/api-docs/swagger.json' target={'_blank'}>/api-docs/swagger.json</NavItem>
              <NavItem eventKey={3} href='#' onClick={this.openAboutModal}>About</NavItem>
            </Nav>
          </Navbar>
        </Col>
      </Row>

      <Modal show={this.state.showTechStackModal} onHide={this.closeTechStackModal}>
        <Modal.Header closeButton>
          <Modal.Title>Tech STack</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <article id='techstack'>
            <h2>Tech Stack</h2>
            <ul>
              <li><a href='http://www.scala-sbt.org/'>SBT</a> and <a href='https://maven.apache.org/'>Maven</a> builds
              </li>
              <li><a href='https://webpack.github.io/'>Webpack 3</a> with <a href='http://gulpjs.com/'>GulpJS 3</a></li>
              <li><a href='https://www.scala-lang.org/'>Scala 2.12</a></li>
              <li><a href='https://www.typescriptlang.org/'>TypeScript 2.5</a></li>
              <li><a href='http://akka.io/'>Akka</a> with <a
                href='http://doc.akka.io/docs/akka/current/scala/stream/index.html'>Akka Streams</a> and <a
                href='http://doc.akka.io/docs/akka-http/current/scala/http/index.html'>Akka HTTP</a></li>
              <li><a href='https://swagger.io/'>Swagger</a></li>
              <li><a href='https://facebook.github.io/immutable-js/'>Immutable JS</a> with <a
                href='https://github.com/rangle/typed-immutable-record'>typed-immutable-record</a> and <a
                href='https://github.com/gajus/redux-immutable'>redux-immutable</a></li>
              <li><a href='http://reactivex.io/rxjs/'>RxJS 5</a></li>
              <li><a href='https://www.w3.org/TR/websockets/'>WebSocket</a></li>
              <li><a href='https://facebook.github.io/jest/'>Jest</a></li>
              <li><a href='https://facebook.github.io/react/'>React</a> with <a
                href='https://react-bootstrap.github.io/'>React Bootstrap</a> and <a
                href='http://redux.js.org/docs/basics/UsageWithReact.html'>React Redux</a></li>
              <li><a href='http://redux.js.org/'>Redux</a> with <a href='https://redux-observable.js.org/'>redux-observable</a>
                and <a href='https://github.com/piotrwitek/react-redux-typescript'>react-redux-typescript</a></li>
              <li><a href='http://visjs.org/'>Vis.js</a></li>
              <li><a href='https://necolas.github.io/normalize.css/'>normalize.css</a></li>
              <li><a href='https://github.com/reactjs/reselect'>reselect</a></li>
              <li><a href='https://github.com/JedWatson/classnames'>classnames</a></li>
              <li><a href='http://lesscss.org/'>Less CSS</a></li>
            </ul>
          </article>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this.closeTechStackModal}>Close</Button>
        </Modal.Footer>
      </Modal>

      <Modal show={this.state.showAboutModal} onHide={this.closeAboutModal}>
        <Modal.Header closeButton>
          <Modal.Title>Tech STack</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <article id='about'>
            <h2>About</h2>
            <ul>
              <li>Network graph of nodes with realtime health over WebSocket</li>
              <li>Scales to thousands of nodes/edges with hundreds updated per second</li>
              <li>StatusBar displayed above network chart</li>
              <li>ListView of all nodes with filter on label</li>
              <li>Detail view of currently selected node</li>
              <li>WebSocket auto-reconnects when online</li>
              <li>swagger.json hyperlink (all served with CORS headers)</li>
              <li>Tech Stack displayed in modal</li>
              <li>About displayed in modal</li>
            </ul>
          </article>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this.closeAboutModal}>Close</Button>
        </Modal.Footer>
      </Modal>

      <Row>
        <Col>
          <Digraph/>
        </Col>
      </Row>
    </Grid>);
  }

}
