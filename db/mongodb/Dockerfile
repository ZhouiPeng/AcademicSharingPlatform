FROM mongo:6.0
COPY ./initdb /docker-entrypoint-initdb.d/
RUN chmod -R 755 /docker-entrypoint-initdb.d
CMD ["mongod"]
