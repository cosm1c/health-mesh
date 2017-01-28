Akka HTTP Webpack Seed
======================

*NOTE*: If IntelliJ has compile errors such as

    [...] is already defined as object [...]
then set Intellij's Scala Compiler "Incrementality type" to SBT (caused by generated Java code).

Tech Stack:
* [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala.html) (Scala 2.12)
* [RxJS 5](http://reactivex.io/rxjs/)
* [TypeScript 2.1](https://www.typescriptlang.org/)
* [Webpack 1](https://webpack.github.io/) within [GulpJS 3](http://gulpjs.com/)
* [WebSocket](https://www.w3.org/TR/websockets/)
* [Protobuf 3 (Java and JavaScript)](https://developers.google.com/protocol-buffers/)
* [SigmaJS](http://sigmajs.org/)


# Development Environment

Terminal 1 - start Akka HTTP Server:

    sbt run

Terminal 2 - start WebPack Dev Server:

    cd ui && gulp webpack-dev-server


# Release Command

    sbt clean assembly
