function gameDots(teamKey, years){
    d3.json("http://localhost:9000/api/teamgames/"+teamKey, function(err, teamGames) {
        const margin = {top:5 , left: 25, bottom: 25, right: 25};
        const height = 120;
        const width = 800;
        const padding = 30;
        const dateParser = d3.time.format("%Y-%m-%d");
        const dateTimeParser = d3.time.format("%Y-%m-%dT%H:%M:%S");
        const scaleMap={};
        years.forEach(function(s){
            scaleMap[s]=d3.time.scale().domain([dateTimeParser.parse((s-1)+"-11-01T12:00:00"), dateTimeParser.parse(s+"-04-30T12:00:00")]).range([0,width])
        });
        const yScale = d3.scale.ordinal().domain(years).rangePoints([height , 0],1.0);
        const wlScale = d3.scale.ordinal().domain(["W","L"]).range(["#080", "#F33"]);

        const xAxis = d3.svg.axis()
            .scale(scaleMap[Math.max(... years)])
            .orient("bottom");
        const yAxis = d3.svg.axis()
            .scale(yScale)
            .tickSize(-width)
            .orient("left");
        const svg = d3.select("div#gameDotsDiv").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom);

        const g = svg
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


        g.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate("+margin.left+"," + height + ")")
            .call(xAxis)
        g.append("g")
            .attr("class", "y axis")
            .attr("transform", "translate("+margin.left+",0)")
            .call(yAxis)

        g.selectAll().data(teamGames).enter().append("circle")
            .attr("cx", function (d) {
                var xd = dateParser.parse(d.date);
                return scaleMap[d.season](xd)
            })
            .attr("cy", function (d) {
                return yScale(d.season);
            })
            .attr("r", 6)
            .attr("fill", function(d){return wlScale(d.wonLost);})
            .attr("transform", "translate("+padding+",0)")
            .attr("stroke","#000")
            .attr("stroke-width", "1px")
            .attr("opacity", 0.9)
            .append("title")
            .text(function(d) { return d.date+"  "+d.wonLost+" "+d.score+"-"+d.oppScore+" "+d.atVs+" "+d.oppName+" "+d.longDate+" "+d.time+" "+d.location});
    });
}
