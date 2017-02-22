#!/bin/bash -eux

tar zxfv build-bundle/*.tar.gz -C build && cp build/*.jar target/
