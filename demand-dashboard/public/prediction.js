$(function() {
  // todo: apply code to demand popup on map
//  $("#demand-form").submit(function(event) {
//    event.preventDefault();
//    var datetime = $("#date-select").val() + " " + $("#time-select").val();
//    var eventTime = new Date(datetime);
//    console.log(eventTime);
//
//    var postConfig = {
//      url: $("#demand-form").attr("action"),
//      data: JSON.stringify({
//        eventTime: eventTime
//      }),
//      contentType : 'application/json',
//      success: function(data) {
//        $("#demand-value").text(data.demand);
//      }
//    };
//
//    $.post(postConfig);
//  });

  // todo: use a default center based on data or get user's location
  var mapCenter = [-73.9440917, 40.7682802];
  $("#latitude-input").val(mapCenter[1]);
  $("#longitude-input").val(mapCenter[0]);

  var predictionMap = new mapboxgl.Map({
    container: 'prediction-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  predictionMap.on('click', function(data) {
    var lat = data.lngLat.lat;
    var lng = data.lngLat.lng;

    $("#latitude-input").val(lat);
    $("#longitude-input").val(lng);

    try {
      predictionMap.removeSource("demand");
      predictionMap.removeLayer("unclustered-points");
    }
    catch (e) {
      // ignored
    }

    predictionMap.addSource("demand", {
      type: "geojson",
      data: "/demand?lng=" + lng + "&lat=" + lat,
      cluster: true,
      clusterMaxZoom: 15,
      clusterRadius: 20
    });

    var layers = [
      [0, 'green'],
      [20, 'orange'],
      [200, 'red']
    ];

    layers.forEach(function (layer, i) {
      try {
        map.removeLayer("cluster-" + i);
      }
      catch (e) {
        // ignored
      }

      predictionMap.addLayer({
        "id": "cluster-" + i,
        "type": "circle",
        "source": "demand",
        "paint": {
          "circle-color": layer[1],
          "circle-radius": 70,
          "circle-blur": 1
        },
        "filter": i === layers.length - 1 ?
          [">=", "point_count", layer[0]] :
          ["all",
          [">=", "point_count", layer[0]],
          ["<", "point_count", layers[i + 1][0]]]
      }, 'waterway-label');
    });

    predictionMap.addLayer({
      "id": "unclustered-points",
      "type": "circle",
      "source": "demand",
      "paint": {
        "circle-color": 'rgba(0,255,0,0.5)',
        "circle-radius": 20,
        "circle-blur": 1
      },
      "filter": ["!=", "cluster", true]
    }, 'waterway-label');

    //todo: make call to predict
    new mapboxgl.Popup()
      .setLngLat(data.lngLat)
      .setHTML('<h2>Demand: 0</h2>')
      .addTo(predictionMap);
  });
});
