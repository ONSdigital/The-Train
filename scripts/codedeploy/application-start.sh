#!/bin/bash

ECR_REPOSITORY_URI=
GIT_COMMIT=

docker run -d --name the-train-$GIT_COMMIT         \
    --volume=/var/babbage/publishing:/transactions \
    --volume=/var/babbage/site:/content            \
    --volume=/var/babbage/tmp:/tmp                 \
    --env=thetrain.transactions=/transactions      \
    --env=thetrain.website=/content                \
    --net=website                                  \
    --restart=always                               \
    $ECR_REPOSITORY_URI/the-train:$GIT_COMMIT
