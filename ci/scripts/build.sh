#!/bin/bash -eux

pushd the-train
  mvn clean package dependency:copy-dependencies -DskipTests=true
popd

cp -r the-train/target/* target/
