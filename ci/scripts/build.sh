#!/bin/bash -eux

pushd the-train
  make build
  cp -r Dockerfile.concourse target/* ../build/
popd
