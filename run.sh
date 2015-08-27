#!/bin/bash
mkdir target/website
mkdir target/transactions
JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
java $JAVA_OPTS \
          -Dthetrain.website=target/website \
          -Dthetrain.transactions=target/transactions \
          -DPORT=8080 \
          -Drestolino.files=src/main/web \
          -Drestolino.classes=target/classes \
          -Drestolino.packageprefix=com.github.davidcarboni.thetrain.api \
          -cp "target/dependency/*" \
          com.github.davidcarboni.restolino.Main
