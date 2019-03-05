#!/bin/bash
mkdir -p target/website
mkdir -p target/transactions
export WEBSITE="/Users/dave/Desktop/zebedee-data/content/zebedee/master"
export TRANSACTION_STORE="/Users/dave/Desktop/zebedee-data/content/zebedee/transactions"
export PUBLISHING_THREAD_POOL_SIZE=100
export PORT=8084
export DP_COLOURED_LOGGING=true
export DP_LOGGING_FORMAT=pretty_json
export PUBLISHING_THREAD_POOL_SIZE=100

JAVA_OPTS="-Xmx1024m -Xms1024m -Xdebug -Xrunjdwp:transport=dt_socket,address=8004,server=y,suspend=n"

mvn package -DskipTests=true && \
java $JAVA_OPTS -jar target/the-train-0.0.1-SNAPSHOT-jar-with-dependencies.jar
