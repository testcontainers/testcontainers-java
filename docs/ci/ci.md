# Continuous Integration

## GitLab


To be able to run your test container into the pipeline you need to run the job as a docker container (see [Running inside Docker](usage/inside_docker.md)),
so you need Docker-In-Docker (docker:dind) service, run your job in the *host* network and mount the docker's socket. Also if you have "Permission denied" you
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


