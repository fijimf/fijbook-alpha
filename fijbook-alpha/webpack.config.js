'use strict';

var webpack = require('webpack'),
    jsPath  = 'app/assets/javascripts',
    path = require('path'),
    srcPath = path.join(__dirname, 'app/assets/javascripts');

module.exports = {
    mode: 'development',
    target: 'web',
    entry: {
        app: path.join(srcPath, 'app.js')
        //, common: ['react-dom', 'react']
    },
    output: {
        path:path.resolve(__dirname, jsPath, 'xxxxx'),
        publicPath: '',
        filename: '[name].js',
        libraryTarget: 'var',
        library: 'App'
    },

    module: {
        noParse: /jquery|lodash/,
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: "babel-loader"
                }
            }
        ]
    },




    plugins: [
        //new webpack.optimize.CommonsChunkPlugin('common', 'common.js'),
    ]
};