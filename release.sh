#!/bin/bash

set -eou pipefail

mvn release:prepare -Pproprietary-deps

mvn release:perform -Pproprietary-deps