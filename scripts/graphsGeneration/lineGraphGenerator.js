const D3Node = require('d3-node');

function line({
  data,
  title,
  primaryDataSet,
  secondaryDataSet,
  xAxisTile = '',
  yPrimaryAxisTitle = '',
  ySecondaryAxisTitle = '',
  legendYanchor = 200,
  legendXanchor = 220,
  xScaleMax = -1,
  xScaleMin = Number.MAX_SAFE_INTEGER,
  yScalePrimaryMax = -1, 
  yScalePrimaryMin = Number.MAX_SAFE_INTEGER, 
  yScaleSecondaryMax = -1, 
  yScaleSecondaryMin = Number.MAX_SAFE_INTEGER,
  fillArea = false,
  selector: _selector = '#chart',
  container: _container = `
    <style>
    .area-translucent {
      opacity: 0.3;
    }
    </style>
    <div id="container">
      <h2 style="font-family: 'Open Sans', Arial, sans-serif; margin-left: 60px;">` + title + `</h2>
      <div id="chart"></div>
    </div>
  `,
  style: _style = '',
  width: _width = 960,
  height: _height = 500,
  margin: _margin = { top: 20, right: 60, bottom: 60, left: 60 },
  lineWidth: _lineWidth = 2,
  isCurve: _isCurve = true,
  tickSize: _tickSize = 5,
  tickPadding: _tickPadding = 5,
} = {}) {

  const d3n = new D3Node({
    selector: _selector,
    svgStyles: _style,
    container: _container,
  });

  //deep copy of data obj
  const _data = JSON.parse(JSON.stringify(data));;

  const d3 = d3n.d3;

  const width = _width - _margin.left - _margin.right;
  const height = _height - _margin.top - _margin.bottom;

  const svg = d3n.createSVG(_width, _height)
        .append('g')
        .attr('transform', `translate(${_margin.left}, ${_margin.top})`);

  const g = svg.append('g');


  //create primary _dataseries
  _xScaleMax = finddatasetMax(xScaleMax, _data, primaryDataSet, function(o) { return o.key; });
  _xScaleMin = finddatasetMin(xScaleMin, _data, primaryDataSet, function(o) { return o.key; });
  _yScalePrimaryMax = finddatasetMax(yScalePrimaryMax, _data, primaryDataSet, function(o) { return o.value; });
  _yScalePrimaryMin = finddatasetMin(yScalePrimaryMin, _data, primaryDataSet, function(o) { return o.value; });

  const xScaleExtent = [_xScaleMin, _xScaleMax]
  const yScalePrimaryExtent = [_yScalePrimaryMin, _yScalePrimaryMax]

  const xScale = d3.scaleLinear()
      .domain(xScaleExtent)
      .rangeRound([0, width]);
  const yScale = d3.scaleLinear()
      .domain(yScalePrimaryExtent)
      .rangeRound([height, 0]);

  const xAxis = d3.axisBottom(xScale)
        .tickSize(_tickSize)
        .tickPadding(_tickPadding);
  const yAxis = d3.axisLeft(yScale)
        .tickSize(_tickSize)
        .tickPadding(_tickPadding);

  const lineChart = d3.line()
      .x(d => xScale(d.key))
      .y(d => yScale(d.value));

  var area = d3.area()
      .x(function(d) { return xScale(d.key); })
      .y0(height)
      .y1(function(d) { return yScale(d.value); });

  if (_isCurve){
    lineChart.curve(d3.curveBasis);
    area.curve(d3.curveBasis);
  } 

  g.append('g')
    .attr('transform', `translate(0, ${height})`)
    .call(xAxis);

  g.append('g').call(yAxis);

  primaryDataSet.forEach(dataset => {
    g.append('g')
    .attr('fill', 'none')
    .attr('stroke-width', _lineWidth)
    .selectAll('path')
    .data([_data[dataset.selector]])
    .enter().append("path")
    .attr('stroke', (d, i) => dataset.lineColor)
    .attr('d', lineChart);

    g.append("circle").attr("cx",width - legendXanchor).attr("cy",legendYanchor).attr("r", 6).style("fill", dataset.lineColor)
    g.append("text").attr("x", width - (legendXanchor -20)).attr("y", legendYanchor + 5).text(dataset.legend).style("font-size", "15px").style("font-family","'Open Sans', Arial, sans-serif").attr("alignment-baseline","middle")

    if(fillArea){
      g.append("path")
      .data([_data[dataset.selector]])
      .attr("class", "area-translucent")
      .attr('fill', (d, i) => dataset.lineColor)
      .attr("d", area);
    }
  

    legendYanchor += 30;
  });


    g.append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 0 - _margin.left)
    .attr("x",0 - (height / 2))
    .attr("dy", "1em")
    .style("text-anchor", "middle")
    .style("font-family","'Open Sans', Arial, sans-serif")
    .text(yPrimaryAxisTitle); 

    g.append("text")             
    .attr("transform",
          "translate(" + (width/2) + " ," + 
                         (height + _margin.top + 30) + ")")
    .style("text-anchor", "middle")
    .style("font-family","'Open Sans', Arial, sans-serif")
    .text(xAxisTile);

    if (secondaryDataSet != undefined){
      //create Secondary _dataseries
      _yScaleSecondaryMax = finddatasetMax(yScaleSecondaryMax, _data, secondaryDataSet, function(o) { return o.value; });
      const secondaryScaleFactor = _yScalePrimaryMax / _yScaleSecondaryMax;
  
      const ySecondaryScale = d3.scaleLinear()
          .domain([0, _yScaleSecondaryMax])
          .rangeRound([height, 0]);
  
      const yAxis2 = d3.axisRight(ySecondaryScale)
            .tickSize(_tickSize)
            .tickPadding(_tickPadding);
      
      g.append('g')
        .attr("transform", "translate(" + width + " ,0)")
        .call(yAxis2);

      g.append("text")
        .attr("transform", "rotate(-90) translate(0," + (width + 30 ) + ")")
        .attr("x",0 - (height / 2))
        .attr("dy", "1em")
        .style("text-anchor", "middle")
        .style("font-family","'Open Sans', Arial, sans-serif")
        .text(ySecondaryAxisTitle); 


        secondaryDataSet.forEach(dataset => {
        //re-scale secondary dataset
        _data[dataset.selector] = _data[dataset.selector].map((val, index, arr) => {return {key: val.key, value: val.value * secondaryScaleFactor}});

        g.append('g')
        .attr('fill', 'none')
        .attr('stroke-width', _lineWidth)
        .selectAll('path')
        .data([_data[dataset.selector]])
        .enter().append("path")
        .attr('stroke', (d, i) => dataset.lineColor)
        .attr('d', lineChart);
    
        g.append("circle").attr("cx",width - legendXanchor).attr("cy",legendYanchor).attr("r", 6).style("fill", dataset.lineColor)
        g.append("text").attr("x", width - (legendXanchor -20)).attr("y", legendYanchor + 5).text(dataset.legend).style("font-size", "15px").style("font-family","'Open Sans', Arial, sans-serif").attr("alignment-baseline","middle")
      
        legendYanchor += 30;
      });
          
    }
  

  return d3n;
}

function finddatasetMax(scaleMax, _data, dataset, mappingFunction){
  if(scaleMax == -1){
    dataset.forEach(dataset => {
      if (_data[dataset.selector] == undefined){
        console.error("dataset not found: " + dataset.selector);
      }
      scaleMax = Math.max(scaleMax, Math.max.apply(Math, _data[dataset.selector].map(o => mappingFunction(o))));
    });
  }
  return scaleMax;
}

function finddatasetMin(scaleMin, _data, dataset, mappingFunction){
  if(scaleMin == Number.MAX_SAFE_INTEGER){
    dataset.forEach(dataset => {scaleMin = Math.min(scaleMin, Math.min.apply(Math, _data[dataset.selector].map(o => mappingFunction(o))))});
  }
  return scaleMin;
}

module.exports = line;
