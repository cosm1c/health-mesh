'use strict';

const path = require('path'),
  webpack = require('webpack'),
  HtmlWebpackPlugin = require('html-webpack-plugin'),
  LessPluginCleanCSS = require('less-plugin-clean-css'),
  ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {

  entry: './app/main.ts',

  output: {
    path: path.join(__dirname, "dist", "ui"),
    filename: "[name].js",
    publicPath: "http://localhost:9090/"
  },

  devtool: "source-map",

  resolve: {
    extensions: ["", ".ts", ".js"]
  },

  plugins: [
    new webpack.DefinePlugin({
      'ENV': JSON.stringify('development')
    }),
    new ExtractTextPlugin('[name].css'),
    new HtmlWebpackPlugin({
      template: 'index.html',
      inject: true
    })
  ],

  module: {
    preLoaders: [
      {
        test: /\.js$/,
        loader: "source-map-loader"
      }
    ],
    loaders: [
      {
        test: /\.less$/,
        loader: ExtractTextPlugin.extract(
          'css?sourceMap!less?sourceMap'
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
