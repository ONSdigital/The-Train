SHELL=bash

JAVA_OPTS=-Xmx1024m -Xms1024m -Xdebug -Xrunjdwp:transport=dt_socket,address=8004,server=y,suspend=n

WEBSITE_DIR:=${WEBSITE}
WEBSITE_DEFAULT:="target/website"

TRANSATIONS_DIR:=${TRANSACTION_STORE}
TRANSATIONS_DEFAULT:="target/transactions"

BIND_ADDR:=${PORT}
BIND_ADDR_DEFAULT:=8084

POOL_SIZE:=${PUBLISHING_THREAD_POOL_SIZE}
POOL_SIZE_DEFAULT:=100

ifndef WEBSITE_DIR
$(warning WEBSITE env var not found applying default: $(WEBSITE_DEFAULT))
WEBSITE_DIR = ${WEBSITE_DEFAULT}
endif

ifndef TRANSATIONS_DIR
$(warning TRANSACTION_STORE env var not found applying default: ${TRANSATIONS_DEFAULT})
TRANSATIONS_DIR = ${TRANSATIONS_DEFAULT}
endif

ifndef BIND_ADDR
$(warning PORT env var not found applying default: $(BIND_ADDR_DEFAULT))
BIND_ADDR = ${BIND_ADDR_DEFAULT}
endif

ifndef POOL_SIZE
$(warning PUBLISHING_THREAD_POOL_SIZE env var not found applying default: ${POOL_SIZE_DEFAULT})
POOL_SIZE = ${POOL_SIZE_DEFAULT}
endif

ensure_dirs:
	@if [[ $(WEBSITE_DIR) == $(WEBSITE_DEFAULT) ]]; then mkdir -p $(WEBSITE_DEFAULT); fi

	@if [[ $(WEBSITE_DIR) == $(WEBSITE_DEFAULT) ]]; then mkdir -p $(TRANSATIONS_DEFAULT); fi
build:
	mvn -DskipTests -Dossindex.skip clean package
debug: build ensure_dirs
	 WEBSITE=${WEBSITE_DIR} TRANSACTION_STORE=${TRANSATIONS_DIR} PORT=${BIND_ADDR} PUBLISHING_THREAD_POOL_SIZE=${POOL_SIZE} java ${JAVA_OPTS} -jar target/the-train-*.jar
test:
	mvn -Dossindex.skip test
audit:
	mvn ossindex:audit
.PHONY: build debug test vault acceptance audit ensure_dirs


