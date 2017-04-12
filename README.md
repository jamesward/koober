Koober
----------------

An uber data pipeline sample app.  Play Framework, Akka Streams, Kafka, Flink, Spark Streaming, and Cassandra.


Start Kafka:

    ./sbt kafkaServer/run

Web App:

1. Obtain an API key from [mapbox.com](https://www.mapbox.com/)
1. Start the Play web app: `MAPBOX_ACCESS_TOKEN=YOUR-MAPBOX-API-KEY ./sbt webapp/run`

Try it out:

1. Open the driver UI: [http://localhost:9000/driver](http://localhost:9000/driver)
1. Open the rider UI: [http://localhost:9000/rider](http://localhost:9000/rider)
1. In the Rider UI, click on the map to position the rider
1. In the Driver UI, click on the rider to initiate a pickup

Start Flink:

1. `./sbt flinkClient/run`
1. Initiate a few picks and see the average pickup wait time change

Start Cassandra:

    ./sbt cassandraServer/run

Start the Spark Streaming process:

1. `./sbt kafkaToCassandra/run`
1. Watch all of the ride data be micro-batched from Kafka to Cassandra

Setup PredictionIO Pipeline:

1. Setup PIO
1. Set the PIO Access Key:

        export PIO_ACCESS_KEY=<YOUR PIO ACCESS KEY>

1. Start the PIO Pipeline:

        ./sbt pioClient/run

Copy demo data into Kafka or PIO:

    For fake data, run:
    
        ./sbt "demoData/run <kafka|pio> fake <number of records> <number of months> <number of clusters>"
        
    For New York data, run:
    
        ./sbt "demoData/run <kafka|pio> ny <number of months> <sample rate>"

Start the Demand Dashboard

    PREDICTIONIO_URL=http://asdf.com MAPBOX_ACCESS_TOKEN=YOUR_MAPBOX_TOKEN ./sbt demandDashboard/run
