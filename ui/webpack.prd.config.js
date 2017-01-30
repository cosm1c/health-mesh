'use strict';

const path = require('path'),
  webpack = require('webpack'),
  HtmlWebpackPlugin = require('html-webpack-plugin'),
  LessPluginCleanCSS = require('less-plugin-clean-css'),
  ExtractTextPlugin = require("extract-text-webpack-plugin"),
  StatsPlugin = require('stats-webpack-plugin'),
  failPlugin = require('webpack-fail-plugin');

const ENV = process.env.NODE_ENV = process.env.ENV = 'production';

module.exports = {

  entry: './app/main.ts',

  output: {
    path: path.join(__dirname, "..", "target", "classes", "ui"),
    filename: '[name]-[hash].min.js',
    chunkFilename: "[id]-[hash].chunk.min.js",
    publicPath: './'
  },

  plugins: [
    failPlugin,
    new webpack.NoErrorsPlugin(),
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.OccurenceOrderPlugin(),
    new HtmlWebpackPlugin({
      template: 'index.html',
      inject: true,
      minify: {
        collapseInlineTagWhitespace: false,
        collapseWhitespace: true,
        // conservativeCollapse: true,
        removeComments: true,
        minifyCSS: true,
        minifyJS: true,
        sortAttributes: true,
        sortClassName: true
      }
    }),
    new ExtractTextPlugin('[name]-[hash].min.css'),
    new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false,
        screw_ie8: true/*,
         lint: true,
         'if_return': true,
         'join_vars': true,
         cascade: true,
         'dead_code': true,
         conditionals: true,
         booleans: true,
         loops: true,
         unused: true*/
      }
    }),
    new StatsPlugin('webpack.stats.json', {
      source: false,
      modules: false
    }),
    new webpack.DefinePlugin({
      'ENV': JSON.stringify(ENV)
    })
  ],

  resolve: {
    extensions: ["", ".ts", ".js"]
  },

  module: {
    loaders: [
      {
        test: /\.less$/,
        loader: ExtractTextPlugin.extract(
          "css!less?strictMath&noIeCompat&compress"
        )
      },
      {
        test: /\.ts$/,
        loader: "ts-loader",
        exclude: /node_modules/
      },
      {
        test: /sigma.*\.js?$/,
        exclude: ['.'],
        loaders: ['script']
      }
    ]
  },

  lessLoader: {
    lessPlugins: [
      new LessPluginCleanCSS({
        advanced: true,
        sourceMapInlineSources: true
      })
    ]
  }
};
