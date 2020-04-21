#!/bin/bash

CORES=$1
SERVER=$2
CLIENT=$3

PROJ_DIR=$(pwd)

jbang ./scripts/resultsProcessing/ProcessResults.java -c $CORES -i $PROJ_DIR/results/data/$CLIENT:$PROJ_DIR/results/data/$SERVER -o  $PROJ_DIR/results/runResult.json

cd ./scripts/graphsGeneration/
mkdir $PROJ_DIR/results/graphs
npm i
node ./genAllGraphsFromConfig.js $PROJ_DIR/scripts/graphsGeneration/graphConf.json $PROJ_DIR/results/runResult.json $PROJ_DIR/results/graphs $CORES

cd $PROJ_DIR

