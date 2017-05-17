#!/bin/bash

if [ "$SUB_APP" = "pio-engine" ]; then

  if [ "$PIO_HOME" = "" ]; then
    echo "Downloading PredictionIO"
    wget https://${BUCKETEER_BUCKET_NAME}.s3.amazonaws.com/public/PredictionIO-0.11.0-incubating.zip
    echo "Unzipping"
    unzip PredictionIO-0.11.0-incubating.zip
    PIO_HOME=~/PredictionIO-0.11.0-incubating
  fi

  if [ "$DATABASE_URL" != "" ]; then
    # from: http://stackoverflow.com/a/17287984/77409
    # extract the protocol
    proto="`echo $DATABASE_URL | grep '://' | sed -e's,^\(.*://\).*,\1,g'`"
    # remove the protocol
    url=`echo $DATABASE_URL | sed -e s,$proto,,g`

    # extract the user and password (if any)
    userpass="`echo $url | grep @ | cut -d@ -f1`"
    pass=`echo $userpass | grep : | cut -d: -f2`
    if [ -n "$pass" ]; then
        user=`echo $userpass | grep : | cut -d: -f1`
    else
        user=$userpass
    fi

    # extract the host -- updated
    hostport=`echo $url | sed -e s,$userpass@,,g | cut -d/ -f1`
    port=`echo $hostport | grep : | cut -d: -f2`
    if [ -n "$port" ]; then
        host=`echo $hostport | grep : | cut -d: -f1`
    else
        host=$hostport
    fi

    # extract the path (if any)
    path="`echo $url | grep / | cut -d/ -f2-`"

    echo "PIO_STORAGE_SOURCES_PGSQL_URL=jdbc:postgresql://$hostport/$path" >> $PIO_HOME/conf/pio-env.sh
    echo "PIO_STORAGE_SOURCES_PGSQL_USERNAME=$user" >> $PIO_HOME/conf/pio-env.sh
    echo "PIO_STORAGE_SOURCES_PGSQL_PASSWORD=$pass" >> $PIO_HOME/conf/pio-env.sh
  fi

  if [ "$BUCKETEER_AWS_ACCESS_KEY_ID" != "" ]; then
    echo "PIO_STORAGE_REPOSITORIES_MODELDATA_SOURCE=S3" >> $PIO_HOME/conf/pio-env.sh
    echo "PIO_STORAGE_SOURCES_S3_TYPE=pio.s3" >> $PIO_HOME/conf/pio-env.sh
    echo "PIO_STORAGE_SOURCES_S3_ACCESS_KEY_ID=${BUCKETEER_AWS_ACCESS_KEY_ID}" >> $PIO_HOME/conf/pio-env.sh
    echo "PIO_STORAGE_SOURCES_S3_SECRET_ACCESS_KEY=${BUCKETEER_AWS_SECRET_ACCESS_KEY}" >> $PIO_HOME/conf/pio-env.sh
    echo "PIO_STORAGE_SOURCES_S3_BUCKET_NAME=${BUCKETEER_BUCKET_NAME}" >> $PIO_HOME/conf/pio-env.sh
  fi

  cp pio-s3/target/scala-2.11/pio-s3.jar $PIO_HOME/lib/spark

  cd pio-engine

  if [ "$1" = "web" ]; then
    echo "Temporarily bind port $PORT to avoid Heroku boot timeout"

    mkdir tmp
    pushd tmp
      python -m SimpleHTTPServer $PORT > /dev/null & echo $! > pid
    popd
  fi

  $PIO_HOME/bin/pio build

  if [ "$1" = "web" ]; then
    echo "Build finished. Cleaning up the temporary port bind"
    cat tmp/pid | xargs kill -9
    rm -r tmp/

    echo "Starting the PredictionIO Service"
    if [ "$PORT" = "" ]; then
      $PIO_HOME/bin/pio deploy
    else
      $PIO_HOME/bin/pio deploy --port $PORT
    fi
  fi

  if [ "$1" = "train" ]; then
    $PIO_HOME/bin/pio train
  fi
elif [ "$SUB_APP" = "demand-dashboard" ]; then
  demand-dashboard/target/universal/stage/bin/demand-dashboard -Dhttp.port=${PORT}
fi
