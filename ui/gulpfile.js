'use strict';

const gulp = require('gulp'),
  gutil = require('gulp-util');

gulp.task('default', function (cb) {
  gutil.log('Tasks are:');
  gutil.log('\tclean');
  gutil.log('\ttest');
  gutil.log('\tpackage');
  cb();
});

gulp.task('clean', function (cb) {
  require("rimraf")('../target/classes/ui', cb);
});

gulp.task('test', function (done) {
  require('npm-run').exec('jest', {cwd: __dirname + '/..'}, done);
});

gulp.task('package', ['clean', 'test'], function (cb) {
  const webpackConfig = require('./webpack.prd.config.js'),
    webpack = require('webpack');

  webpack(webpackConfig, function (err, stats) {
    // see: https://webpack.github.io/docs/node.js-api.html#stats-tostring
    gutil.log("[webpack stats]", stats.toString());
    if (err) throw err;
    if (stats.hasErrors()) throw 'Webpack failed to compile';
    cb();
  });
});
