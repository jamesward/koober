#!/bin/bash

if [ "$SUB_APP" = "pio-engine" ]; then
  echo "Downloading PredictionIO"
  wget https://${BUCKETEER_BUCKET_NAME}.s3.amazonaws.com/public/PredictionIO-0.11.0-incubating.zip
  echo "Unzipping"
  unzip PredictionIO-0.11.0-incubating.zip
  PIO_HOME=~/PredictionIO-0.11.0-incubating
  cd pio-engine
  $PIO_HOME/pio deploy --port $PORT
fi
