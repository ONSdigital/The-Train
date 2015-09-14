#!/bin/bash
mkdir -p target/website
mkdir -p target/transactions
JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8004,server=y,suspend=n"

mvn package && \
java $JAVA_OPTS \
          -Dthetrain.website=target/website \
          -Dthetrain.transactions=target/transactions \
          -DPORT=8084 \
          -Drestolino.packageprefix=com.github.davidcarboni.thetrain.api \
          -jar target/the-train-destination-0.0.1-SNAPSHOT-jar-with-dependencies.jar

## Reloadable
#java $JAVA_OPTS \
#          -Dthetrain.website=target/website \
#          -Dthetrain.transactions=target/transactions \
#          -DPORT=8080 \
#          -Drestolino.files=src/main/web \
#          -Drestolino.classes=target/classes \
#          -Drestolino.packageprefix=com.github.davidcarboni.thetrain.api \
#          -cp "target/dependency/*" \
#          com.github.davidcarboni.restolino.Main
