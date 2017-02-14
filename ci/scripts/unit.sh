#!/bin/bash -eux

pushd the-train
  mvn clean surefire:test
popd
