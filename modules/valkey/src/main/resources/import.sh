#!/usr/bin/env bash
set -e
valkey-cli $([[ -n "$1" ]] && echo "-a $1") < "/tmp/import.valkey"
echo "Imported"
