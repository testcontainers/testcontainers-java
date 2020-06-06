#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
set -o xtrace

if [ -z $1 ] ; then
  echo "First parameter (commit SHA) is required!" && exit 1;
fi

STATUS=$(curl -s https://api.github.com/repos/testcontainers/testcontainers-java/commits/$1/status | jq -r '.state')

[ "$STATUS" == 'success' ]
