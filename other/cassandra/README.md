# Cassandra schema

This folder contains the structure of the cassandra database required for BBData, as well as dockerfiles and data for testing purposes. 

### Cassandra schema

In production, you only need the schema definition, that you can find in `bootstrap_data/schema.cql`.


### Dev setup (docker)

__Important__: if you change the structure or test data (`bootstrap_data/*.cql`), you need to rebuild the image !

Build the image (see `docker-build.sh`):
```bash
docker build -t bbdata-cassandra .
``` 

Launch the image (see `docker-start.sh`):
```bash
 docker run --rm -p 9042:9042 --name bbcassandra bbdata-cassandra
```
or, *if you want to persist the data* between container runs, use:
```bash
 docker run --rm -p 9042:9042 -v $PWD/data:/var/lib/cassandra --name bbcassandra bbdata-cassandra
```

Connect:
```bash
docker exec -it bbcassandra cqlsh
```

If you want more information on how I fixed the problem of initializing the Cassandra container,
have a look at [this gist](https://gist.github.com/derlin/0d4c98f7787140805793d6268dae8440).