#!/bin/bash -eux

pushd the-train
  mvn -DskipTests=true ossindex.skip=true clean package dependency:copy-dependencies
  cp -r Dockerfile.concourse target/* ../build/
popd
