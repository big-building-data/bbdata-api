#!/usr/bin/env bash

CONTAINER=cassandra

docker run -d --rm \
    --name $CONTAINER \
    -p 9042:9042 \
    -v $PWD/data:/var/lib/cassandra \
    cassandra:3.11