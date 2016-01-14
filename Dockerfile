from onsdigital/java-component

# Consul
WORKDIR /etc/consul.d
RUN echo '{"service": {"name": "thetrain", "tags": ["blue"], "port": 8080, "check": {"script": "curl http://localhost:8080 >/dev/null 2>&1", "interval": "10s"}}}' > thetrain.json

# Add the built artifact
WORKDIR /usr/src
ADD git_commit_id /usr/src/
ADD ./target/*-jar-with-dependencies.jar /usr/src/target/

# Update the entry point script
# Set the entry point
ENTRYPOINT java -Xmx4094m \
          -Drestolino.files="target/web" \
          -Drestolino.packageprefix=com.github.davidcarboni.thetrain.api \
          -jar target/*-jar-with-dependencies.jar
