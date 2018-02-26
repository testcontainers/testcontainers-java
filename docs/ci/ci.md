# Continuous Integration

## GitLab


To be able to run your test container into the pipeline you need to run the job as a docker container (see [Running inside Docker](../usage/inside_docker.md)),
so you need Docker-In-Docker (docker:dind) service and mount the docker's socket. Also if you have "Permission denied" you
need to run your container as root user:

```yml
# to improve performance
variables:
  DOCKER_DRIVER: overlay2

# we need DinD service
services:
  - docker:dind

test:
 image: docker:latest
 stage: test
 script:
   - >
      docker run -t --rm 
      -v /var/run/docker.sock:/var/run/docker.sock
      -v "$(pwd)":"$(pwd)"
      -w "$(pwd)"
      -u 0:0
      gradle:3.4 ./gradlew test
 only:
   - master
```

In this job we run in a multiline command a docker container:
* using the official gradle image
* to be delete when finished
* use the root user if you are running a Shared Runner


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
