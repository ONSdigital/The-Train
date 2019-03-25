#!/bin/bash
mkdir -p target/website
mkdir -p target/transactions

# The port of the underlying HTTP server
export PORT=8084

# The path of the website content, for dev local set up this will be your Zebedee content "master" dir path.
export content_path="<YOUR_PATH>"

# A path to a directory to create the publishing transactions under. For dev local setup this is typically under your
# zebedee root
export transactions_path="<YOUR_PATH>"

# The dp-logging library has config for enabling coloured output (make it easier to read). To disable it either set to
# false or remove the property.
export DP_COLOURED_LOGGING=true

# dp-logging also allows you to choose a log output format. The available options are:
# text (default)
# json
# pretty_json
# Set this var to suit your needs.
export DP_LOGGING_FORMAT=pretty_json

JAVA_OPTS="-Xmx1024m -Xdebug -Xrunjdwp:transport=dt_socket,address=8004,server=y,suspend=n"

mvn package -DskipTests=true && \
java $JAVA_OPTS \
          -Dthetrain.website=$content_path \
          -Dthetrain.transactions=$transactions_path \
          -DPORT=$PORT \
          -Drestolino.packageprefix=com.github.davidcarboni.thetrain.api \
          -jar target/the-train-0.0.1-SNAPSHOT-jar-with-dependencies.jar