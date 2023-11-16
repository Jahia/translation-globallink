const path = require('path');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {ProvidePlugin} = require('webpack');
const {CycloneDxWebpackPlugin} = require('@cyclonedx/webpack-plugin');

/** @type {import('@cyclonedx/webpack-plugin').CycloneDxWebpackPluginOptions} */
const cycloneDxWebpackPluginOptions = {
    specVersion: '1.4',
    rootComponentType: 'library',
    outputLocation: './bom-assets'
};

module.exports = (env, argv) => {
    let config = {
        entry: {
            assets: {import: path.resolve(__dirname, 'src/javascript/assets.js')}
        },
        output: {
            path: path.resolve(__dirname, 'src/main/resources/javascript/apps/assets/'),
            filename: 'assets-translations-globallink.bundle.js',
            chunkFilename: '[name].assets-translations-globallink.[chunkhash:6].js'
        },
        plugins: [
            new ProvidePlugin({
                $: 'jquery',
                jQuery: 'jquery'
            }),
            new CleanWebpackPlugin({verbose: false}),
            new CopyWebpackPlugin({patterns: [{from: './package.json', to: ''}]}),
            new CycloneDxWebpackPlugin(cycloneDxWebpackPluginOptions)
        ],
        resolve: {
            mainFields: ['module', 'assets'],
            extensions: ['.mjs', '.js', '.jsx', '.json', '.css'],
            alias: {
                jquery: 'jquery/src/jquery',
                'jquery-form': 'jquery-form/src/jquery.form.js',
                'jquery.quicksearch': 'jquery.quicksearch/src/jquery.quicksearch.js',
                multiselect: 'multiselect/js/jquery.multi-select.js'
            },
            modules: [
                path.resolve(__dirname, 'src/javascript'),
                path.resolve(__dirname, 'node_modules')
            ]
        },
        optimization: {
            moduleIds: 'deterministic',
            splitChunks: {
                maxSize: 400000
            }
        },
        module: {
            rules: [
                {
                    test: /\.jsx?$/,
                    include: [path.join(__dirname, 'src')],
                    loader: 'babel-loader',
                    options: {
                        presets: [
                            ['@babel/preset-env', {modules: false, targets: {safari: '7', ie: '10'}}],
                            '@babel/preset-react'
                        ],
                        plugins: [
                            '@babel/plugin-syntax-dynamic-import'
                        ]
                    }
                },
                {
                    test: /\.css$/i,
                    use: ['style-loader', 'css-loader']
                }
            ]
        },
        mode: argv.mode || 'development'
    };

    config.devtool = (argv.mode === 'production') ? 'source-map' : 'eval-source-map';

    if (argv.analyze) {
        config.devtool = 'source-map';
        config.plugins.push(new BundleAnalyzerPlugin());
    }

    return config;
};
