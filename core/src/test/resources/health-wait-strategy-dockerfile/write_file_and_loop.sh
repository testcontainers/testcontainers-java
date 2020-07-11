#!/bin/ash

set -e

echo sleeping
sleep 2
echo writing file
touch /testfile
echo wrote file

while true; do sleep 1; done
