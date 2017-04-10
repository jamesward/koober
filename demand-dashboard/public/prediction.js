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

    predictionMap.addControl(new mapboxgl.NavigationControl({position: 'top-left'}));

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
      data: "/demand?lng=" + lng + "&lat=" + lat
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
                    [2.0, '#fee5d9'],
                    [4.0, '#fcae91'],
                    [6.0, '#fb6a4a'],
                    [8.0, '#de2d26'],
                    [10.0, '#a50f15']
                  ]
          },
          "circle-radius": {
              'base': 1.75,
              'stops': [[12, 3], [22, 180]]
          },
          'circle-opacity' : 0.8
        }
      });


    //todo: make call to predict
    new mapboxgl.Popup()
      .setLngLat(data.lngLat)
      .setHTML('<h2>Demand: 0</h2>')
      .addTo(predictionMap);

//     var features = predictionMap.queryRenderedFeatures(e.point, { layers: ['prediction'] });
//     if (!features.length) {
//            return;
//        }
//
//     var feature = features[0];
//     var popup = new mapboxgl.Popup()
//             .setLngLat(feature.geometry.coordinates)
//             .setHTML('<h2>Demand: '+ feature.properties['demand']+'</h2>')
//             .addTo(map);
  });


//     predictionMap.on('mousemove', function(e) {
//         var features = predictionMap.queryRenderedFeatures(e.point, { layers: ['prediction'] });
//         predictionMap.getCanvas().style.cursor = (features.length) ? 'pointer' : '';
//     });
});