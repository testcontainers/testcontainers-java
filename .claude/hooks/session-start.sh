#!/bin/bash
# Prepares a Claude Code on the web container for building and testing this
# repository: a Java 17 toolchain, a running Docker daemon for Testcontainers,
# and a warmed Gradle distribution.
set -euo pipefail

# The local developer machine is already set up by its owner; only the
# ephemeral remote containers need provisioning.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

# build.gradle pins the toolchain to Java 17. The images ship a newer JDK, and
# Gradle's foojay auto-provisioning cannot reach api.foojay.io through the
# egress proxy, so the toolchain has to come from the distro packages.
if [ ! -x /usr/lib/jvm/java-17-openjdk-amd64/bin/javac ]; then
  echo "Installing the Java 17 toolchain..."
  $SUDO apt-get update -qq
  $SUDO env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk-headless
fi

# Testcontainers needs a Docker daemon. The image ships the CLI and dockerd but
# starts no daemon, so every session has to launch one.
if ! docker info >/dev/null 2>&1; then
  echo "Starting the Docker daemon..."
  $SUDO sh -c 'nohup dockerd >/var/log/dockerd.log 2>&1 &'
  for _ in $(seq 1 30); do
    if docker info >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
fi

if ! docker info >/dev/null 2>&1; then
  echo "The Docker daemon did not come up; Testcontainers tests will fail." >&2
  $SUDO tail -n 20 /var/log/dockerd.log >&2 || true
  exit 1
fi

# Downloading the Gradle distribution takes about a minute, and the container
# state is cached afterwards, so pay for it here rather than in the first build.
# A warm-up failure is not worth failing the session over.
"${CLAUDE_PROJECT_DIR:-$(dirname "$0")/../..}/gradlew" --version >/dev/null 2>&1 || true

echo "Ready: Java 17 toolchain, Docker daemon and Gradle distribution are available."
