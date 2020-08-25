FROM cassandra:3.11

# fix UTF-8 accents in init scripts
ENV LANG C.UTF-8

# add scripts to initialize the db
COPY bootstrap_data/*.csv /docker-entrypoint-initdb.d/
COPY bootstrap_data/schema.cql /docker-entrypoint-initdb.d/01.cql
COPY bootstrap_data/import_data.cql /docker-entrypoint-initdb.d/02.cql

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD ["cassandra", "-f"]