#/bin/bash

[ $(curl -s https://api.github.com/repos/testcontainers/testcontainers-java/commits/$1/status | jq -r '.state') == 'success' ]
