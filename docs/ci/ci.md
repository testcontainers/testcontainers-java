# Continuous Integration

## GitLab

In order to use Testcontainers in a Gitlab CI pipeline, you need to run the job as a Docker container (see [Running inside Docker](../usage/inside_docker.md)).
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

## CircleCI 2.0

Your CircleCI configuration should use a dedicated VM for testcontainers to work. You can achieve this by specifying the 
executor type in your `.circleci/config.yml` to be `machine` instead of the default `docker` executor ( see [Choosing an Executor Type](https://circleci.com/docs/2.0/executor-types/) for more info ).  

Here is a sample CircleCI configuration that does a checkout of a project and runs maven:

```yml
# Check https://circleci.com/docs/2.0/language-java/ for more details
#
version: 2
executorType: machine
jobs:
  build:
    steps:
      - checkout

      - run: mvn -B clean install
```
