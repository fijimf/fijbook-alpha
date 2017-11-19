function showHistogram(url, legend, divId) {
    var showKDP = true; // show the kernel density plot?
    var bandwith = 4; // bandwith (smoothing constant) h of the kernel density estimator

    var margin = {top: 20, right: 30, bottom: 30, left: 50},
        width = 300 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom;

// the x-scale parameters
// the histogram function

    d3.json(url, function (error, statRawData) {
//        console.table(statRawData);
        var statvals = statRawData.map(function (x) {
            return x["value"]
        });
//        console.table(statvals);

        var xmin = d3.min(statvals);
        var xmax = d3.max(statvals);
        var x = d3.scale.linear()
            .domain([xmin < 0 ? xmin * 1.05 : xmin * 0.95, xmax > 0 ? xmax * 1.05 : xmax * 0.95]).nice()
            .range([0, width]);

        // the y-scale parameters
        var xAxis = d3.svg.axis()
            .scale(x)
            .orient("bottom").tickValues([xmin,xmax]);
        var svg = d3.select("#"+divId).append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        // draw the background
        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis)
            .append("text")
            .attr("class", "label")
            .attr("x", width/2)
            .attr("y", -height)
            .style("text-anchor", "middle")
            .style("font-size", "100%")
            .text(legend);


        var distinctValues = d3.map(statvals, function (d) {
            return d;
        }).size();

        var histogram = d3.layout.histogram()
            .frequency(true).bins(distinctValues < 20 ? distinctValues : 20);

        var data = histogram(statvals);

        ymax = d3.max(data, function (d) {
            return d.y;
        });

        var y = d3.scale.linear()
            .domain([0, ymax + 1])
            .range([height, 0]);


        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left");

        var line = d3.svg.line()
            .x(function (d) {
                return x(d[0]);
            })
            .y(function (d) {
                return y(d[1]);
            });

        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis);

        console.log(data)
        //var kde = kernelDensityEstimator(epanechnikovKernel(7), x.ticks(100));
        var kde = kernelDensityEstimator(epanechnikovKernel(bandwith), x.ticks(100));

        svg.selectAll(".bar")
            .data(data)
            .enter().insert("rect", ".axis")
            .attr("class", "bar")
            .attr("x", function (d) {
                return x(d.x) + 1;
            })
            .attr("y", function (d) {
                return y(d.y);
            })
            .attr("width", x(data[0].dx + data[0].x) - x(data[0].x) - 1)
            .attr("height", function (d) {
                return height - y(d.y);
            });

        // show the kernel density plot
        if (showKDP == true) {
            svg.append("path")
                .datum(kde(data))
                .attr("class", "line")
                .attr("d", line);
        }
    });
}

function kernelDensityEstimator(kernel, x) {
    return function (sample) {
        return x.map(function (x) {
            //console.log(x + " ... " + d3.mean(sample, function(v) { return kernel(x - v); }));
            return [x, d3.mean(sample, function (v) {
                return kernel(x - v);
            })];
        });
    };
}

function epanechnikovKernel(bandwith) {
    return function (u) {
        //return Math.abs(u /= bandwith) <= 1 ? .75 * (1 - u * u) / bandwith : 0;
        if (Math.abs(u = u / bandwith) <= 1) {
            return 0.75 * (1 - u * u) / bandwith;
        } else return 0;
    };
}
