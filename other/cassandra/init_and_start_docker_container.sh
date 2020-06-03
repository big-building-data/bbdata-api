#!/usr/bin/env bash

CONTAINER=cassandra

# run container, mounting the bootstrap_data as well
docker run -d --rm \
    --name $CONTAINER \
    -p 9042:9042 \
    -v $PWD/bootstrap_data:/bootstrap_data \
    -v $PWD/data:/var/lib/cassandra \
    cassandra:3.11

# wait a bit
sleep 20

# use cqlsh to generatre keyspace
docker exec -i $CONTAINER cqlsh -f /bootstrap_data/schema.cql 

# use cqlsh to import data
docker exec -i $CONTAINER cqlsh -k bbdata2 -f /bootstrap_data/import_data.cql