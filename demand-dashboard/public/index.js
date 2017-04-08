$(function() {
  var now = new Date();

  var fifteenMinutesAgo = new Date();
  fifteenMinutesAgo.setMinutes(-15);

  $("#startDateTime").val(fifteenMinutesAgo.toJSON().slice(0,19));
  $("#endDateTime").val(now.toJSON().slice(0,19));

  $("#demandForm").submit(function(event) {
    event.preventDefault();
    var startDateTime = new Date($("#startDateTime").val());
    var endDateTime = new Date($("#endDateTime").val());

    var postConfig = {
      url: $("#demandForm").attr("action"),
      data: JSON.stringify({
        startDateTime: startDateTime,
        endDateTime: endDateTime
      }),
      contentType : 'application/json',
      success: function(data) {
        $("#output").text("Demand = " + data.demand);
      }
    };

    $.post(postConfig);
  });

  var map = new mapboxgl.Map({
    container: 'map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: [-73.9440917, 40.7682802]
  });

  map.on('click', function(data) {
    var lng = data.lngLat.lng;
    var lat = data.lngLat.lat;

    try {
      map.removeSource("demand");
      map.removeLayer("unclustered-points");
    }
    catch (e) {
      // ignored
    }

    map.addSource("demand", {
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

      map.addLayer({
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

    map.addLayer({
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
  });

});
