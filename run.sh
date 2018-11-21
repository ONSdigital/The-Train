#!/bin/bash
mkdir -p target/website
mkdir -p target/transactions
export content_path="/Users/dave/Desktop/zebedee-data/content/zebedee/master"
export transactions_path="/Users/dave/Desktop/zebedee-data/content/zebedee/transactions"
export DP_COLOURED_LOGGING=true
export DP_LOGGING_FORMAT=pretty_json
export POOL_SIZE=100

JAVA_OPTS="-Xmx1024m -Xms1024m -Xdebug -Xrunjdwp:transport=dt_socket,address=8004,server=y,suspend=n"

mvn package -DskipTests=true && \
java $JAVA_OPTS \
          -Dthetrain.website=$content_path \
          -Dthetrain.transactions=$transactions_path \
          -DPORT=8084 \
          -jar target/the-train-0.0.1-SNAPSHOT-jar-with-dependencies.jar
