$(function () {
    $(document).ready(function () {
        Highcharts.setOptions({
            global: {
                useUTC: false
            }
        });

        var globalJson,
            serverOnline = false;

        var generateSplineOptions = function (title, jsonElement, seriesName) {
            return {
                chart: {
                    type: 'spline',
                    animation: Highcharts.svg, // don't animate in old IE
                    events: {
                        load: function () {

                            // set up the updating of the chart each second
                            var profiles = this.series[0],
                                maxSamples = 20,
                                count = 0;
                            setInterval(function () {
                                var x = (new Date()).getTime(),
                                    y = globalJson[jsonElement];
                                profiles.addPoint([x, y], true, (++count >= maxSamples));
                            }, 1000);

                        }
                    }
                },
                title: {
                    text: title
                },
                xAxis: {
                    type: 'datetime',
                    tickPixelInterval: 150
                },
                yAxis: {
                    title: {
                        text: 'Number of Requests'
                    },
                    plotLines: [{
                        value: 0,
                        width: 1,
                        color: '#808080'
                }]
                },
                tooltip: {
                    formatter: function () {
                        return '<b>' + this.series.name + '</b><br/>' +
                            Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) + '<br/>' +
                            Highcharts.numberFormat(this.y, 2);
                    }
                },
                legend: {
                    enabled: false
                },
                exporting: {
                    enabled: false
                },
                series: [
                    {
                        name: seriesName,
                        data: []
                }
            ]
            }
        }


        setInterval(function () {
            $.getJSON("http://localhost:8081/debug", function (json) {
                    globalJson = json;
                    $('#online').toggle(true);
                    $('#offline').toggle(false);

                    $('#numProfiles').text(globalJson["put-profiles"]);
                    $('#numLikes').text(globalJson["likes"]);
                })
                .fail(function () {
                    globalJson = null;
                    $('#offline').toggle(true);
                    $('#online').toggle(false);
                });
        }, 1000);


        $('#requestsPerSecond').highcharts(generateSplineOptions("Total Requests Per Second", "all-requestPersecond", "Avg Requests"));
        $('#getsPerSecond').highcharts(generateSplineOptions("GET's Per Second", "get-requestPersecond", "Avg GET's"));
        $('#postsPerSecond').highcharts(generateSplineOptions("POST's Per Second", "post-requestPersecond", "Avg POST's"));
        $('#putsPerSecond').highcharts(generateSplineOptions("PUT's Per Second", "put-requestPersecond", "Avg PUT's"));
        $('#deletesPerSecond').highcharts(generateSplineOptions("DELETES's Per Second", "delete-requestPersecond", "Avg DELETE's"));


    });
});