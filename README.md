# Health-Mesh #

An example of displaying a network of nodes in a digraph with health.
Supports Jenkins Blue Ocean with Jenkinsfile.
 
Tech Stack:
* [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala.html) (Scala 2.12)
* [Swagger](http://swagger.io/)
* [RxJS 5](http://reactivex.io/rxjs/)
* [TypeScript 2.1](https://www.typescriptlang.org/)
* [Webpack 1](https://webpack.github.io/) within [GulpJS 3](http://gulpjs.com/)
* [WebSocket](https://www.w3.org/TR/websockets/)
* [Protobuf 3 (Java and JavaScript)](https://developers.google.com/protocol-buffers/)
* [SigmaJS](http://sigmajs.org/)

## Jenkinsfile
See: [Using a Jenkinsfile](https://jenkins.io/doc/book/pipeline/jenkinsfile/)
Documentation also available in a running Jenkins instance: [http://localhost:8080/pipeline-syntax/](http://localhost:8080/pipeline-syntax/).


## SBT Development Environment ##

Terminal 1 - start Akka HTTP Server:

    sbt run

Terminal 2 - start WebPack Dev Server:

    cd ui && gulp webpack-dev-server


## SBT Release Command ##

    sbt clean assembly


## Maven Development Environment ##

Terminal 1 - start Akka HTTP Server:

    mvn exec:java

Terminal 2 - start WebPack Dev Server:

    cd ui && gulp webpack-dev-server


## Maven Release Command ##

    mvn
