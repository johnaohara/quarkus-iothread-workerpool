var myArgs = process.argv.slice(2);
console.log('myArgs: ', myArgs);

const graphConf = myArgs[0];
const dataFile = myArgs[1];
const outputDir = myArgs[2];
const cores = myArgs[3];

const fs = require('fs');
const output = require('d3node-output');
const d3nLine = require('./lineGraphGenerator');

let data = JSON.parse(fs.readFileSync(dataFile));
let config = JSON.parse(fs.readFileSync(graphConf))


config.forEach(graphConfig => {
    console.info('Generating charts for: ' + graphConfig.output);
    output(outputDir + '/' + graphConfig.output , d3nLine(
        { data: data.graphData
            , title: graphConfig.title + '<br/>' + cores + ' cores'
            , primaryDataSet: graphConfig.primaryDataset
            , secondaryDataSet: graphConfig.secondaryDataset
            , legendXanchor: graphConfig.legendXanchor
            , legendYanchor: graphConfig.legendYanchor
            , yScalePrimaryMin: graphConfig.yScalePrimaryMin
            , yScalePrimaryMax: graphConfig.yScalePrimaryMax
            , xAxisTile: graphConfig.xAxisTile
            , yPrimaryAxisTitle: graphConfig.yPrimaryAxisTitle
            , ySecondaryAxisTitle: graphConfig.ySecondaryAxisTitle
            , fillArea: graphConfig.fillArea
            }), { width: 960, height: 600 }
        );
});
