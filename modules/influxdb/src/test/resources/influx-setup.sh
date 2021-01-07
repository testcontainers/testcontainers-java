#!/bin/bash
influx setup \
       --force \
       --username "${INFLUXDB_USER}" \
       --password "${INFLUXDB_PASSWORD}" \
       --bucket "${INFLUXDB_BUCKET}" \
       --org "${INFLUXDB_ORG}" \
       --retention "${INFLUXDB_RETENTION}""${INFLUXDB_RETENTION_UNIT}"
