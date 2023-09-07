#!/usr/bin/env bash

# Wait for testcontainer's injected redpanda config with the port only known after docker start
until grep -q "# Injected by testcontainers" "/etc/redpanda/redpanda.yaml"
do
  sleep 0.1
done
exec /entrypoint.sh $@