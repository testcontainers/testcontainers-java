# GitLab CI

In order to use Testcontainers in a Gitlab CI pipeline, you need to run the job as a Docker container (see [Patterns for running inside Docker](dind_patterns.md)).
So edit your `.gitlab-ci.yml` to include the [Docker-In-Docker service](https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#use-docker-in-docker-executor) (`docker:dind`) and set the `DOCKER_HOST` variable to `tcp://docker:2375`.

Here is a sample `.gitlab-ci.yml` that executes test with gradle:

```yml
# DinD service is required for Testcontainers
services:
  - docker:dind

variables:
  # Instruct Testcontainers to use the daemon of DinD.
  DOCKER_HOST: "tcp://docker:2375"
  # Improve performance with overlayfs.
  DOCKER_DRIVER: overlay2

test:
 image: gradle:5.0
 stage: test
 script: ./gradlew test
```
