docker run -d --rm \
    --name cassandra \
    -p 9042:9042 \
    -v $PWD/data:/var/lib/cassandra \
    cassandra:3.11
