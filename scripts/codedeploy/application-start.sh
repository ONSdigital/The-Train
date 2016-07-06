#!/bin/bash

ECR_REPOSITORY_URI=
GIT_COMMIT=

docker run -d                                    \
  --env=thetrain.transactions=/transactions      \
  --env=thetrain.website=/content                \
  --name=the-train                               \
  --net=website                                  \
  --restart=always                               \
  --volume=/var/babbage/publishing:/transactions \
  --volume=/var/babbage/site:/content            \
  --volume=/var/babbage/tmp:/tmp                 \
  $ECR_REPOSITORY_URI/the-train:$GIT_COMMIT
