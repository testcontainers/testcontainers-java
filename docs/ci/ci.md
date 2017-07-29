# Continuous Integration

## Gitlab

[Gitlab Runners](https://docs.gitlab.com/ee/ci/runners/README.html) run the code defined in your `.gitlab-ci.yml` project's file.

You can define different stages (build, test, deploy ...) and different jobs in each stage. 
Also, you can run every job with a particular Docker image and start images as services, making all of them a powerful CI tool.

This is a typical pipeline example where three jobs compile, check and deploy your gradle project:

```yml
image: gradle:3.4

build:
 stage: build
 script:
   - ./gradlew compile
 only:
   - master

test_business:
 stage: test
 script:
   - ./gradlew check 
 only:
   - master

pages:
  stage: deploy
  script:
    - ./gradlew asciidoc  --no-daemon
    - mkdir public
    - cp -R build/asciidoc/html5/* public
  artifacts:
    paths:
    - public
  only:
   - master
```

To be able to run your @testcontainers into the pipeline you need to run the job as a docker container (see [Running inside Docker](usage/inside_docker.html)),
so you need DockerInDocker (docker:dind) service, run your job in the *host* network and mount the docker's socket. Also if you have "Permission denied" you
need to run your container as root user:

```yml
# to improve performance
variables:
  DOCKER_DRIVER: overlay2

# we need DinD service
services:
  - docker:dind

# Run our @testcontainers  
test_container:
 image: docker:latest
 stage: test
 script:
   - >
      docker run -t --rm --net=host
      -v /var/run/docker.sock:/var/run/docker.sock
      -v "$(pwd)":"$(pwd)"
      -w "$(pwd)"
      -u 0:0
      gradle:3.4 ./gradlew -DintegrationTest.single=ContainerPingServiceSpec
      check --no-daemon -i
 only:
   - master
```

In this job we run in a multiline command a docker container:
* using the official gradle image
* to be delete when finished
* run in the *host* network to be able to connect with the docker service
* use the root user if you are running a Shared Runner
* we are interesed only in our TestContainer test


