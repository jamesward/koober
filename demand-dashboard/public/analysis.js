$(function() {
  // todo: use a default center based on data
  var mapCenter = [-73.9440917, 40.7682802];

  var actualDemandMap = new mapboxgl.Map({
    container: 'actual-demand-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  var gradientBoostedTreesMap = new mapboxgl.Map({
    container: 'gradient-boosted-trees-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  var linearRegressionMap = new mapboxgl.Map({
    container: 'linear-regression-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  var ridgeRegressionMap = new mapboxgl.Map({
    container: 'ridge-regression-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  var neuralNetworkMap = new mapboxgl.Map({
    container: 'neural-network-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  var randomForestMap = new mapboxgl.Map({
    container: 'random-forest-map',
    style: 'mapbox://styles/mapbox/streets-v9',
    zoom: 11,
    center: mapCenter
  });

  var algorithmMapNames = {"algGBTree":gradientBoostedTreesMap, "algRegression":linearRegressionMap, "ridgeRegression":ridgeRegressionMap, "randomForest":randomForestMap};
  var coordinates = [];
  var updateAlgorithmMapSliderDelay = 200; //Milliseconds
  var updateAlgorithmMapInitialDelay = 1000; //Milliseconds

  $('#datetimepicker').datepicker({
    format: 'mm/dd/yyyy',
    autoclose: true
  });
  var startDate = new Date(2016, 5, 7);
  $('#datetimepicker').datepicker('setDate', startDate);

  $('#time-slider').change(function(e) {
    var input = parseInt(e.target.value);
    current_time = input;
    $('#time-slider-value').html(prettyNumbers(Math.floor(input / 2)) + ":" + prettyNumbers((input % 2) * 30) + "-" +
      (prettyNumbers(Math.floor((input + 1) / 2))) + ":" + prettyNumbers(((input + 1) % 2) * 30))
    updateAllMaps(updateAlgorithmMapSliderDelay)
  });

  function getTimeElements() {
    var datepickerTokens = $("#datetimepicker").val().split("/");
    var month = parseInt(datepickerTokens[0])-1;
    var day = parseInt(datepickerTokens[1]);
    var year = parseInt(datepickerTokens[2]);
    var timeInterval = parseInt($('#time-slider').val());
    var hour = parseInt(timeInterval / 2);
    var minute = 30 * (timeInterval - hour * 2);
    return [year, month, day, hour, minute];
  }

  function getNormalizedTime() {
    [year, month, day, hour, minute] = getTimeElements();
    return Date.UTC(year, month, day, hour, minute, 0, 0) / (1000 * 60 * 30);
  }

  function getDateTime() {
    [year, month, day, hour, minute] = getTimeElements();
    return new Date(year, month, day, hour, minute, 0, 0);
  }

  function buildQueryString(algorithm, coordinates) {
    return "algorithm=" + algorithm + "&eventTime=" + getDateTime().toISOString() + "&coordinates=" + JSON.stringify(coordinates)
  }

  function prettyNumbers(number) {
    var result = number.toString()
    if (result.length == 1) {
      return "0" + result
    }
    return result
  }

  function filterBy(time) {
    var filters = ['==', 'time', time];
    actualDemandMap.setFilter('actual', filters);
  }

  actualDemandMap.on('load', function() {
    actualDemandMap.addSource("actualDemand", {
      type: "geojson",
      data: geoJson
    });

    actualDemandMap.addLayer({
      "id": "actual",
      "type": "circle",
      "source": "actualDemand",
      "paint": {
        "circle-color": {
          property: 'demand',
          stops: [
            [1.0, '#FADBD8'],
            [2.0, '#F1948A'],
            [3.0, '#E74C3C'],
            [4.0, '#B03A2E'],
            [5.0, '#78281F']
          ]
        },
        "circle-radius": {
          'base': 8,
          "property": "demand",
          'stops': [
            [0.0, 8],
            [1.0, 10],
            [2.0, 12],
            [3.0, 14],
            [4.0, 16],
            [5.0, 20]
          ]
        },
        'circle-opacity': 1.0
      }
    });
    updateAllMaps(updateAlgorithmMapInitialDelay)
  });

  function updateAllMaps(delay) {
    filterBy(getNormalizedTime().toString())
    setTimeout(function() {
      for (var algorithmName in algorithmMapNames) {
            updateMapPredictions(algorithmMapNames[algorithmName],algorithmName);
      }
    }, delay);
  }


  function updateMapPredictions(algorithmMap, algorithmName) {
    updateCoordinates();
    if (!coordinates || coordinates.length == 0) {
      return;
    }
    var sourceName = "source" + algorithmName;
    var layerName = "layer" + algorithmName;

    try {
      algorithmMap.removeSource(sourceName);
      algorithmMap.removeLayer(layerName);
    } catch (e) {
      // ignored
    }

    algorithmMap.addSource(sourceName, {
      type: "geojson",
      data: "/analyze?" + buildQueryString(algorithmName, coordinates)
    });

    algorithmMap.addLayer({
      "id": layerName,
      "type": "circle",
      "source": sourceName,
      "paint": {
        "circle-color": {
          property: 'demand',
          stops: [
            [14.0, '#FADBD8'],
            [18.0, '#F1948A'],
            [20.0, '#E74C3C'],
            [24.0, '#B03A2E'],
            [30.0, '#78281F']
          ]
        },
        "circle-radius": {
          'base': 8,
          "property": "demand",
          'stops': [
            [14.0, 8],
            [16.0, 10],
            [18.0, 12],
            [20.0, 14],
            [22.0, 16],
            [25.0, 18],
            [30.0, 20]
          ]
        },
        'circle-opacity': 1.0
      }
    });
  }


  function updateCoordinates() {
      var features = actualDemandMap.queryRenderedFeatures({
        layers: ['actual']
      });
      coordinates = [];
      if (features) {
        for (var i = 0; i < features.length; i++) {
          coordinates.push(features[i].geometry.coordinates);
        }
      }
  }

});