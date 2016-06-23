#!/usr/bin/env sh

while true; do
    echo "Exposing env on port 3000"
    env | nc -l -p 3000
done