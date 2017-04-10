$(function() {
  // todo: use a default center based on data
  var mapCenter = [-73.9440917, 40.7682802];
  $("#latitude-input").val(mapCenter[1]);
  $("#longitude-input").val(mapCenter[0]);

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

  var multinomialLogicalRegressionMap = new mapboxgl.Map({
      container: 'multinomial-logical-regression-map',
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
});