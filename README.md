# Health-Mesh #

Network graph of nodes with realtime health over WebSocket.
Supports Jenkins Blue Ocean for building with Jenkinsfile.
 
Tech Stack:
* [SBT](http://www.scala-sbt.org/) and [Maven](https://maven.apache.org/) builds 
* [Webpack 3](https://webpack.github.io/) with [GulpJS 3](http://gulpjs.com/)
* [Scala 2.12](https://www.scala-lang.org/)
* [TypeScript 2.5](https://www.typescriptlang.org/)
* [Akka](http://akka.io/) with [Akka Streams](http://doc.akka.io/docs/akka/current/scala/stream/index.html)
 and [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/index.html)
* [RxJava](https://github.com/ReactiveX/RxJava)
* [Swagger](https://swagger.io/)
* [Immutable JS](https://facebook.github.io/immutable-js/)
 with [typed-immutable-record](https://github.com/rangle/typed-immutable-record)
  and [redux-immutable](https://github.com/gajus/redux-immutable)
* [RxJS 5](http://reactivex.io/rxjs/)
* [WebSocket](https://www.w3.org/TR/websockets/)
* [Jest](https://facebook.github.io/jest/)
* [React](https://facebook.github.io/react/) with [React Bootstrap](https://react-bootstrap.github.io/)
 and [React Redux](http://redux.js.org/docs/basics/UsageWithReact.html)
* [Redux](http://redux.js.org/) with [redux-observable](https://redux-observable.js.org/)
 and [react-redux-typescript](https://github.com/piotrwitek/react-redux-typescript)
* [Vis.js](http://visjs.org/)
* [reselect](https://github.com/reactjs/reselect)
* [classnames](https://github.com/JedWatson/classnames)
* [Less CSS](http://lesscss.org/)


## Jenkinsfile
See: [Using a Jenkinsfile](https://jenkins.io/doc/book/pipeline/jenkinsfile/)
Documentation also available in a running Jenkins instance:
[http://localhost:8080/pipeline-syntax/](http://localhost:8080/pipeline-syntax/).


## SBT Development Environment ##

Terminal 1 - start Akka HTTP Server:

    sbt run

Terminal 2 - start WebPack Dev Server:

    npm run webpack-dev-server


## SBT Release Command ##

    sbt clean assembly


## Maven Development Environment ##

Terminal 1 - start Akka HTTP Server:

    mvn compile exec:java

Terminal 2 - start WebPack Dev Server:

    npm run webpack-dev-server


## Maven Release Command ##

    mvn
