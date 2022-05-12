FROM postgres:9.6.12 AS dumper

COPY somepath/init_postgresql.sql /docker-entrypoint-initdb.d

RUN ["sed", "-i", "s/exec \"$@\"/echo \"skipping...\"/", "/usr/local/bin/docker-entrypoint.sh"]

ENV POSTGRES_USER=test
ENV POSTGRES_PASSWORD=test
ENV PGDATA=/data

RUN ["/usr/local/bin/docker-entrypoint.sh", "postgres"]

FROM postgres:9.6.12

COPY --from=dumper /data $PGDATA
