#!/bin/bash

GIT_COMMIT=

if [[ $(docker inspect --format="{{ .State.Running }}" the-train-$GIT_COMMIT) == "false" ]]; then
  exit 1;
fi
