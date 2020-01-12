# Cassandra docker container

### Quick start

First, pull the cassandra image (same version as in production): 
```bash
docker pull cassandra:3.11
```

We will use the local directory `./data` to store the cassandra data. Thus, running the container amounts to
(see `start_docker_container.sh`):
```bash
docker run -d --rm \
    --name cassandra \
    -p 9042:9042 \
    -v $PWD/data:/var/lib/cassandra \
    cassandra:3.11
```

### Populating the database

Launch a cassandra container, this time also attaching the directory `bootstrap_data`:
```bash
docker run -d --rm \
    --name cassandra \
    -p 9042:9042 \
    -v $PWD/data:/var/lib/cassandra \
    -v $PWD/bootstrap_data:/bootstrap_data \
    cassandra:3.11
```

Then, what's left is to:
1. create the cassandra schema (`schema.cql`);
2. import the data (`import_data.cql`).

All this can be done using:
```bash
# !! only works if the bootstrap_data directory is mounted inside the container !
# create the schema
docker exec -it cassandra cqlsh -f /bootstrap_data/schema.cql
# import the data
docker exec -it cassandra cqlsh -k bbdata2 -f /bootstrap_data/import_data.cql
```



