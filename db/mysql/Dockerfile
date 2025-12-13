FROM mysql:8.0
COPY ./initdb /docker-entrypoint-initdb.d/
RUN chmod -R 755 /docker-entrypoint-initdb.d
