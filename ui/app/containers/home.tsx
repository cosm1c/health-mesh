import * as React from 'react';
import {Digraph} from '../components';
import {Button, Col, Grid, Modal, Nav, Navbar, NavItem, Row} from 'react-bootstrap';

interface HomeProps {
}

interface HomeState {
  showTechnologiesModal: boolean;
  showAboutModal: boolean;
}

export class Home extends React.Component<HomeProps, HomeState> {

  constructor(props: HomeProps) {
    super(props);
    this.state = {
      showTechnologiesModal: false,
      showAboutModal: false,
    };
    this.openTechnologiesModal = this.openTechnologiesModal.bind(this);
    this.closeTechnologiesModal = this.closeTechnologiesModal.bind(this);
    this.openAboutModal = this.openAboutModal.bind(this);
    this.closeAboutModal = this.closeAboutModal.bind(this);
  }

  openTechnologiesModal() {
    this.setState({showTechnologiesModal: true});
  }

  closeTechnologiesModal() {
    this.setState({showTechnologiesModal: false});
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
              <NavItem eventKey={1} href='#' onClick={this.openTechnologiesModal}>Technologies</NavItem>
              <NavItem eventKey={2} href='api-docs/swagger.json' target='_blank'>api-docs/swagger.json</NavItem>
              <NavItem eventKey={3} href='#' onClick={this.openAboutModal}>About</NavItem>
            </Nav>
          </Navbar>
        </Col>
      </Row>

      <Modal show={this.state.showTechnologiesModal} onHide={this.closeTechnologiesModal}>
        <Modal.Header closeButton>
          <Modal.Title>Technologies</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ul>
            <li>
              Backend
              <ul>
                <li><a href='https://www.scala-lang.org/'>Scala 2.12</a></li>
                <li><a href='http://akka.io/'>Akka</a> with <a
                  href='http://doc.akka.io/docs/akka/current/scala/stream/index.html'>Akka Streams</a> and <a
                  href='http://doc.akka.io/docs/akka-http/current/scala/http/index.html'>Akka HTTP</a></li>
                <li><a href='https://github.com/ReactiveX/RxJava'>RxJava</a></li>
                <li><a href='https://swagger.io/'>Swagger</a></li>
                <li><a href='http://www.scala-sbt.org/'>SBT</a> and <a href='https://maven.apache.org/'>Maven</a> builds
                </li>
              </ul>
            </li>
            <li>
              Frontend
              <ul>
                <li><a href='https://www.typescriptlang.org/'>TypeScript 2.5</a></li>
                <li><a href='https://facebook.github.io/immutable-js/'>Immutable JS</a> with <a
                  href='https://github.com/rangle/typed-immutable-record'>typed-immutable-record</a> and <a
                  href='https://github.com/gajus/redux-immutable'>redux-immutable</a></li>
                <li><a href='http://reactivex.io/rxjs/'>RxJS 5</a> over a single <a
                  href='https://www.w3.org/TR/websockets/'>WebSocket</a></li>
                <li><a href='https://facebook.github.io/react/'>React</a> with <a
                  href='https://react-bootstrap.github.io/'>React Bootstrap</a> and <a
                  href='http://redux.js.org/docs/basics/UsageWithReact.html'>React Redux</a></li>
                <li><a href='http://redux.js.org/'>Redux</a> with <a
                  href='https://redux-observable.js.org/'>redux-observable</a> and <a
                  href='https://github.com/piotrwitek/react-redux-typescript'>react-redux-typescript</a></li>
                <li><a href='https://facebook.github.io/jest/'>Jest</a></li>
                <li><a href='http://visjs.org/'>Vis.js</a></li>
                <li><a href='https://github.com/reactjs/reselect'>reselect</a></li>
                <li><a href='https://github.com/JedWatson/classnames'>classnames</a></li>
                <li><a href='http://lesscss.org/'>Less CSS</a></li>
                <li><a href='https://webpack.github.io/'>Webpack 3</a> with some <a href='http://gulpjs.com/'>GulpJS
                  3</a></li>
              </ul>
            </li>
          </ul>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this.closeTechnologiesModal}>Close</Button>
        </Modal.Footer>
      </Modal>

      <Modal show={this.state.showAboutModal} onHide={this.closeAboutModal}>
        <Modal.Header closeButton>
          <Modal.Title>About</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ul>
            <li>Graph of service dependencies.</li>
            <li>Live updates of topology and health.</li>
            <li>Can request immediate update of a node by initiating a poll.</li>
            <li>WebSocket reconnects when loses connection.</li>
            <li>swagger.json hyperlink (with CORS headers).</li>
          </ul>
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
