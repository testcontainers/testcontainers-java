#!/bin/bash
influx setup \
       -f \
       -u "${INFLUXDB_USER}" \
       -p "${INFLUXDB_PASSWORD}" \
       -b "${INFLUXDB_BUCKET}" \
       -o "${INFLUXDB_ORG}" \
       -r 0
