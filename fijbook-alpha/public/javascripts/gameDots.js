function gameDots(teamKey, years){
    d3.json("http://localhost:9000/api/teamgames/"+teamKey, function(err, teamGames) {
        const margin = {top:5 , left: 25, bottom: 5, right: 25};
        var height = 120;
        var width = 800;
        var padding = 30;
        var dateParser = d3.time.format("%Y-%m-%d");
        var dateTimeParser = d3.time.format("%Y-%m-%dT%H:%M:%S");
        var scaleMap={};
        years.forEach(function(s){
            scaleMap[s]=d3.time.scale().domain([dateTimeParser.parse((s-1)+"-11-01T12:00:00"), dateTimeParser.parse(s+"-04-30T12:00:00")]).range([0,width])
        });
        var yScale = d3.scale.ordinal().domain(years).rangePoints([height , 0],1.0);
        var wlScale = d3.scale.ordinal().domain(["W","L"]).range(["#080", "#F33"]);

        var xAxis = d3.svg.axis()
            .scale(scaleMap[Math.max(... years)])
            .orient("bottom");
        var yAxis = d3.svg.axis()
            .scale(yScale)
            .orient("left");
        var svg = d3.select("div#gameDotsDiv").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom);

        var g = svg
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
            .attr("opacity", function (d) {
                if (d.homeAwayClass==="home-game") return 0.65;
                else if (d.homeAwayClass==="neutral-game") return 0.80;
                else return 0.95;
            })
            .append("title")
            .text(function(d) { return d.date+"  "+d.wonLost+" "+d.score+"-"+d.oppScore+" "+d.atVs+" "+d.oppName+" "+d.longDate+" "+d.time+" "+d.location});
    });
}
