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

gulp.task('test', function (done) {
  const Server = require('karma').Server;

  new Server({
    configFile: __dirname + '/karma.conf.js',
    singleRun: true,
    reporters: ['junit']
  }, done).start();
});

gulp.task('package', ['clean', 'test'], function (cb) {
  const webpackConfig = require('./webpack.prd.config.js');

  webpack(webpackConfig, function (err, stats) {
    if (err || stats.hasErrors()) throw err;
    gutil.log("[webpack]", stats.toString({
      // see: https://webpack.github.io/docs/node.js-api.html#stats-tostring
    }));
    cb(err);
  });
});

gulp.task("webpack-dev-server", ['clean'], function (cb) {
  const WebpackDevServer = require("webpack-dev-server"),
    devWebpackConfig = Object.create(require("./webpack.dev.config.js"));

  new WebpackDevServer(webpack(devWebpackConfig))
    .listen(9090, "localhost", function (err) {
      if (err) throw err;
      gutil.log("[webpack-dev-server]", "http://localhost:9090/webpack-dev-server/index.html");
      cb();
    });
});
