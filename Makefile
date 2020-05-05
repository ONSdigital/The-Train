SHELL=bash

JAVA_OPTS=-Xmx1024m -Xms1024m -Xdebug -Xrunjdwp:transport=dt_socket,address=8004,server=y,suspend=n

## The default website content directory
WEBSITE_DEFAULT:="target/website"

ifndef WEBSITE
$(warning WEBSITE env var not found applying default: $(WEBSITE_DEFAULT))
WEBSITE = ${WEBSITE_DEFAULT}
endif

## The default publish transactions directory
TRANSACTIONS_DEFAULT:="target/transactions"

ifndef TRANSACTION_STORE
$(warning TRANSACTION_STORE env var not found applying default: ${TRANSACTIONS_DEFAULT})
TRANSACTION_STORE = ${TRANSACTIONS_DEFAULT}
endif

## The default bind address
PORT_DEFAULT:=8084

ifndef PORT
$(warning PORT env var not found applying default: $(PORT_DEFAULT))
PORT = ${PORT_DEFAULT}
endif

## The default thread pool size
PUBLISHING_THREAD_POOL_SIZE_DEFAULT:=100

ifndef PUBLISHING_THREAD_POOL_SIZE
$(warning PUBLISHING_THREAD_POOL_SIZE env var not found applying default: ${PUBLISHING_THREAD_POOL_SIZE_DEFAULT})
PUBLISHING_THREAD_POOL_SIZE = ${PUBLISHING_THREAD_POOL_SIZE_DEFAULT}
endif

ensure_dirs:
	@if [[ $(WEBSITE) == $(WEBSITE_DEFAULT) ]]; then mkdir -p $(WEBSITE_DEFAULT); fi
	@if [[ $(TRANSACTION_STORE) == $(TRANSACTIONS_DEFAULT) ]]; then mkdir -p $(TRANSACTIONS_DEFAULT); fi
build:
	mvn -DskipTests -Dossindex.skip clean package
debug: build ensure_dirs
	 @WEBSITE=$(WEBSITE) TRANSACTION_STORE=$(TRANSACTION_STORE) PORT=$(PORT) PUBLISHING_THREAD_POOL_SIZE=$(PUBLISHING_THREAD_POOL_SIZE) java ${JAVA_OPTS} -jar target/the-train-*.jar
test:
	mvn -Dossindex.skip test
audit:
	mvn ossindex:audit
.PHONY: build debug test vault acceptance audit ensure_dirs


