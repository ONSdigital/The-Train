#!/bin/bash

if [[ $(docker inspect --format="{{ .State.Running }}" the-train) == "false" ]]; then
  exit 1;
fi
