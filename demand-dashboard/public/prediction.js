$(function() {
  // todo: apply code to demand popup on map

  // todo: use a default center based on data or get user's location
  var mapCenter = [-73.9440917, 40.7682802];
  $("#latitude-input").val(mapCenter[1]);
  $("#longitude-input").val(mapCenter[0]);

  var date = "2017-03-22"
  var time = "16:00"
  var lat = 40.7682802
  var lng = -73.9440917
  var temperature = 20
  var weather = 0
  var weatherArray = [1,0,0,0,0,0,0]
  var algorithm = 0

  $("#date-select").on("change", function(){
    console.log(typeof($("#date-select").val()))
    date = $("#date-select").val()
   });

  $("#time-select").on("change", function(){
    console.log($("#time-select").val())
    time = $("#time-select").val()
   });

  $("#temperature-input").on("input", function(data){
    temperature = parseFloat($("#temperature-input").val());
    console.log(temperature);
    console.log(typeof(temperature));
  });

  $("#weather-select").on("change", function(){
    weather = parseInt($("#weather-select").val())
    console.log(weather)
    console.log(typeof(weather))
    weatherArray = [0,0,0,0,0,0,0]
    weatherArray[weather] = 1
    console.log(weatherArray)
  });

  $("#algorithm-select").on("change", function(){
      algorithm = parseInt($("#algorithm-select").val())
    });


  var predictionMap = new mapboxgl.Map({
    container: 'prediction-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });


  predictionMap.addControl(new mapboxgl.NavigationControl({position: 'top-left'}));

    var pop = new mapboxgl.Popup()
  predictionMap.on('click', function(data) {

    lat = data.lngLat.lat;
    lng = data.lngLat.lng;
    $("#latitude-input").val(lat);
    $("#longitude-input").val(lng);
    pop.remove()
    pop.setLngLat(data.lngLat)

    try {
      predictionMap.removeSource("demand");
      predictionMap.removeLayer("prediction");
    }
    catch (e) {
      // ignored
    }


    predictionMap.addSource("demand", {
         type: "geojson",
         data: "/predict?" + buildQueryString(lat, lng)
    });
    predictionMap.addLayer({
          "id": "prediction",
          "type": "circle",
          "source": "demand",
          "paint": {
            "circle-color": {
                property: 'demand',
                type: 'exponential',
                stops: [
                      [15.0, '#fee5d9'],
                      [30.0, '#fcae91'],
                      [45.0, '#fb6a4a'],
                      [60.0, '#de2d26'],
                      [75.0, '#a50f15']
                    ]
            },
            "circle-radius": {
                'base': 1.75,
                'stops': [[12, 3], [22, 180]]
            },
            'circle-opacity' : 0.8
          }
      });

  });
    predictionMap.on('data', function (data) {
       try {
           var demands = predictionMap.querySourceFeatures('demand', data.point);
           if (demands[0]){
              pop.setHTML('<h2>Demand:' + demands[0]["properties"]["demand"] + '</h2>')
                   .addTo(predictionMap);
           }
        }
        catch (e) {
          // ignored
//          console.log(e)
        }
    });

    predictionMap.on('mousemove', function (data) {
       try {
//            console.log(data.point)
           var demands = predictionMap.querySourceFeatures('demand', data.point);
           if (demands[0]){
              pop.remove()
              var i = findNearestIndex(demands, data);
              if (i != -1){
                pop.setHTML('<h2>Demand:' + demands[i]["properties"]["demand"] + '</h2>')
                    .addTo(predictionMap);
                pop.setLngLat([demands[i]["geometry"]["coordinates"][0], demands[i]["geometry"]["coordinates"][1]])
              }

           }
        }
        catch (e) {
          // ignored
//              console.log(e)
        }
    });

  function makeCluster(lat, lng) {
    lats = [lat];
    lngs = [lng];
    lats.push(lat + 2*0.0016, lat + 0.0016, lat + 0.0016, lat + 0.0016, lat, lat, lat, lat, lat - 0.0016, lat - 0.0016, lat - 0.0016, lat - 2*0.0016);
    lngs.push(lng, lng - 0.0016, lng, lng + 0.0016, lng - 2*0.0016, lng - 0.0016, lng + 0.0016, lng + 2*0.0016, lng - 0.0016, lng, lng + 0.0016, lng);
    return [lats, lngs];
  }

  function buildQueryJson(lat, lng){
    result = {}
    var datetime = date + " " + time;
    var eventTime = new Date(datetime);
    result = {
    "eventTime":eventTime.toISOString(),
    "lat":lat,
    "lng":lng,
    "temperature":temperature,
    "clear":weatherArray[0],
    "fog":weatherArray[1],
    "rain": weatherArray[2],
    "snow": weatherArray[3],
    "hail": weatherArray[4],
    "thunder":weatherArray[5],
    "tornado":weatherArray[6]
    }
    return JSON.stringify(result)
  }

   function buildQueryString(lat, lng){
       var datetime = date + " " + time;
       var eventTime = new Date(datetime);
       return "eventTime=" + eventTime.toISOString() + "&lat=" + lat +
             "&lng=" + lng + "&temperature=" + temperature + "&clear=" + weatherArray[0] + "&fog=" + weatherArray[1] +
             "&rain=" + weatherArray[2] + "&snow=" + weatherArray[3] + "&hail=" + weatherArray[4]
              + "&thunder=" + weatherArray[5] + "&tornado=" + weatherArray[6] + "&algorithm=" + algorithm
       }
    });

    function findNearestIndex(demands, data){
        var nIndex = 0;
        var nDistance = Number.MAX_SAFE_INTEGER
        for (i = 0; i < demands.length; i++){
            var dist = Math.pow((data.lngLat.lng - demands[i]["geometry"]["coordinates"][0]), 2) + Math.pow((data.lngLat.lat - demands[i]["geometry"]["coordinates"][1]),2);
            if (dist < nDistance){
                nDistance = dist;
                nIndex = i;
            }
        }
        if (nDistance > 0.00002){
            nIndex = -1
            console.log("greater")
        }
        return nIndex;

    }