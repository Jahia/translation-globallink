const path = require('path');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const ModuleFederationPlugin = require("webpack/lib/container/ModuleFederationPlugin");
const moonstone = require("@jahia/moonstone/dist/rulesconfig-wp");
const getModuleFederationConfig = require('@jahia/webpack-config/getModuleFederationConfig');
const packageJson = require('./package.json');

const {CycloneDxWebpackPlugin} = require('@cyclonedx/webpack-plugin');

/** @type {import('@cyclonedx/webpack-plugin').CycloneDxWebpackPluginOptions} */
const cycloneDxWebpackPluginOptions = {
    specVersion: '1.4',
    rootComponentType: 'library',
    outputLocation: './bom'
};

module.exports = (env, argv) => {
    let config = {
        entry: {
            main: path.resolve(__dirname, 'src/javascript/index')
        },
        output: {
            path: path.resolve(__dirname, 'src/main/resources/javascript/apps/'),
            filename: 'translations-globallink.bundle.js',
            chunkFilename: '[name].translations-globallink.[chunkhash:6].js'
        },
        plugins: [
            new ModuleFederationPlugin(getModuleFederationConfig(packageJson, {
                remotes: {
                    '@jahia/jcontent': 'appShell.remotes.jcontent',
                }
            })),
            new CleanWebpackPlugin({verbose: false}),
            new CopyWebpackPlugin({patterns: [{from: './package.json', to: ''}]}),
            new CycloneDxWebpackPlugin(cycloneDxWebpackPluginOptions)
        ],
        resolve: {
            mainFields: ['module', 'main'],
            extensions: ['.mjs', '.js', '.jsx', '.json']
        },
        optimization: {
            moduleIds: 'deterministic',
            splitChunks: {
                maxSize: 400000
            }
        },
        module: {
            rules: [
                ...moonstone,
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
                    use: ["style-loader", "css-loader"],
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
