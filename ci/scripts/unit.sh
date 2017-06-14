#!/bin/bash -eux

pushd the-train
  mvn test
popd
