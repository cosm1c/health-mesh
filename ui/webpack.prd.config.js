'use strict';

const path = require('path'),
  webpack = require('webpack'),
  ExtractTextPlugin = require('extract-text-webpack-plugin'),
  CleanCSSPlugin = require("less-plugin-clean-css"),
  HtmlWebpackPlugin = require('html-webpack-plugin');

const ENV = process.env.NODE_ENV = process.env.ENV = 'production';

const extractLess = new ExtractTextPlugin({
  filename: "[name].[contenthash].css"
});

module.exports = {

  bail: true,

  entry: './app/index.tsx',

  output: {
    filename: '[name].[chunkhash].js',
    path: path.resolve(__dirname, "..", "target", "classes", "ui"),
    publicPath: './'
  },

  module: {
    rules: [
      {
        test: /\.(jpg|png|gif)$/,
        use: 'file-loader'
      },
      {
        test: /\.(woff|woff2|eot|ttf|svg)$/,
        use: {
          loader: 'url-loader',
          options: {
            limit: 100000
          }
        }
      },
      {
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          fallback: "style-loader",
          use: "css-loader"
        })
      },
      {
        test: /\.less$/,
        use: extractLess.extract({
          filename: "[name].[contenthash].css",
          fallback: 'style-loader',
          use: [
            {
              loader: "css-loader"
            },
            {
              loader: "less-loader",
              options: {
                sourceMap: false,
                strictMath: true,
                noIeCompat: true,
                lessPlugins: [
                  new CleanCSSPlugin({advanced: true})
                ]
              }
            }
          ]
        })
      },
      {
        test: /\.html$/,
        loader: 'html-loader'
      },
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/
      }
    ]
  },


  resolve: {
    modules: [__dirname, path.resolve(__dirname, '..', 'node_modules')],
    extensions: ['.ts', '.tsx', '.js', '.jsx', '.css', '.less', '.json']
  },

  context: __dirname,

  target: "web",

  stats: true,

  plugins: [
    new webpack.LoaderOptionsPlugin({
      minimize: true,
      debug: false,
      options: {
        context: __dirname,
        htmlLoader: {
          minimize: true,
          removeAttributeQuotes: false,
          caseSensitive: true
        }
      }
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(ENV),
      IS_PROD: true
    }),
    new ExtractTextPlugin('[name]-[contenthash].min.css'),
    // TODO: use Google Closure instead of UglifyJS
    new webpack.optimize.UglifyJsPlugin({
      beautify: false,
      output: {
        comments: false
      },
      mangle: {
        screw_ie8: true
      },
      compress: {
        screw_ie8: true,
        warnings: false,
        conditionals: true,
        unused: true,
        comparisons: true,
        sequences: true,
        dead_code: true,
        evaluate: true,
        if_return: true,
        join_vars: true,
        negate_iife: false // we need this for lazy v8
      }
    }),
    new HtmlWebpackPlugin({
      template: 'index.html',
      chunksSortMode: 'dependency',
      inject: true,
      hash: true,
      xhtml: true,
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
    })
  ],

  node: {
    global: true,
    crypto: 'empty',
    process: false,
    module: false,
    clearImmediate: false,
    setImmediate: false
  }
};
