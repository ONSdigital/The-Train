#!/bin/bash -eux

pushd the-train
    mvn ossindex:audit
popd