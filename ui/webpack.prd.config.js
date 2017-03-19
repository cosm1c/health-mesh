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

  entry: './app/main.ts',

  output: {
    filename: '[name].[chunkhash].js',
    path: path.resolve(__dirname, "..", "target", "classes", "ui"),
    publicPath: './'
  },

  module: {
    rules: [
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
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          fallback: "style-loader",
          use: "css-loader"
        })
      },
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
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/
      },
      {
        test: /\.html$/,
        loader: 'html-loader'
      },
      {
        rules: [
          {
            test: /sigma.*\.js?$/,
            //exclude: ['.'],
            use: ['script-loader']
          }
        ]
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
        context: __dirname
      }
    }),
    new webpack.DefinePlugin({
      // Any occurrence of process.env.NODE_ENV in the imported code is replaced with "production"
      'process.env.NODE_ENV': JSON.stringify(ENV),
      IS_PROD: true
    }),
    new ExtractTextPlugin('[name]-[contenthash].min.css'),
    new webpack.optimize.UglifyJsPlugin({
      beautify: false,
      mangle: {
        screw_ie8: true,
        keep_fnames: true
      },
      compress: {
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
      },
      comments: false
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
  ]
};
