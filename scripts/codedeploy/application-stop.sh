#!/bin/bash

CONTAINER_ID=$(docker ps | grep 'the-train:[[:alnum:]]\{7\}' | awk '{print $1}')

if [[ -n $CONTAINER_ID ]]; then
  docker stop $CONTAINER_ID
fi
