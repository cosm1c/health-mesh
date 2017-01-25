'use strict';

const gulp = require('gulp'),
  gutil = require('gulp-util'),
  webpack = require('webpack');

gulp.task('default', function (cb) {
  gutil.log('Tasks are:');
  gutil.log('\tclean');
  gutil.log('\twebpack-dev-server');
  gutil.log('\ttest');
  gutil.log('\tpackage');
  cb();
});

gulp.task('clean', function (cb) {
  const rimraf = require("rimraf");

  rimraf('dist', function () {
    rimraf('generated', cb);
  });
});

// TODO: Watch proto src for dev-server
gulp.task('protoc', ['clean'], function (cb) {
  const exec = require('child_process').exec,
    fs = require('fs');

  fs.mkdir('generated', function () {
    exec('protoc --js_out=import_style=commonjs,binary:generated --proto_path=../src/main/protobuf ../src/main/protobuf/*.proto',
      function (error, stdout, stderr) {
        if (error) {
          console.error('protoc error: ', error);
          return;
        }
        if (stdout) {
          console.log('[protoc]  ', stdout);
        }
        if (stderr) {
          console.error('[protoc]! ', stderr);
        }
        cb();
      })
  });
});

gulp.task('test', function (done) {
  const Server = require('karma').Server;

  new Server({
    configFile: __dirname + '/karma.conf.js',
    singleRun: true
  }, done).start();
});

gulp.task('package', ['clean', 'protoc', 'test'], function (cb) {
  const webpackConfig = require('./webpack.prd.config.js');

  webpack(webpackConfig, function (err, stats) {
    if (err) throw err;
    gutil.log("[webpack]", stats.toString({
      // see: https://webpack.github.io/docs/node.js-api.html#stats-tostring
    }));
    cb();
  });
});

gulp.task("webpack-dev-server", ['clean', 'protoc'], function (cb) {
  const WebpackDevServer = require("webpack-dev-server"),
    prodWebpackConfig = Object.create(require("./webpack.dev.config.js"));

  new WebpackDevServer(webpack(prodWebpackConfig), {
    contentBase: "http://localhost:8080",
    quiet: false,
    noInfo: true,
    stats: {
      colors: true
    }
  }).listen(9090, "localhost", function (err) {
    if (err) throw err;
    gutil.log("[webpack-dev-server]", "http://localhost:9090/webpack-dev-server/index.html");
    cb();
  });
});
