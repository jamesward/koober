
object DL4J extends App {

  /*
  //val Nd4j_MAX_SLICES_TO_PRINT = 10
  //val Nd4j_MAX_ELEMENTS_PER_SLICE = 10

  val conf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
    .seed(12345)
    .iterations(20)
    .learningRate(0.1)
    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
    .weightInit(WeightInit.XAVIER)
    .updater(Updater.NESTEROVS).momentum(0.9)
    .list()
    .layer(0, new DenseLayer.Builder().nIn(5).nOut(3)
      .activation(Activation.TANH)
      .build())
    .layer(1, new OutputLayer.Builder(LossFunction.RMSE_XENT)
      .activation(Activation.IDENTITY)
      .nIn(3).nOut(1).build())
    .backprop(true).pretrain(false)
    .build()

  val model: MultiLayerNetwork = new MultiLayerNetwork(conf)
  model.init()
  model.setListeners(new ScoreIterationListener(2))

  model.fit()
  */
  MultiLayerNetworkExternalErrors.main(args)

}
