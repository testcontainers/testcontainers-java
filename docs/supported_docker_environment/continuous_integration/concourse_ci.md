# Concourse CI

This is an example to run Testcontainers tests on [Concourse CI](https://concourse-ci.org/).

A possible `pipeline.yml` config looks like this:

```yaml
resources:
- name: repo
  type: git
  source:
    uri: https://github.com/testcontainers/testcontainers-java-repro.git

jobs:
- name: testcontainers-job
  plan:
  # Add a get step referencing the resource
  - get: repo
  - task: testcontainers-task
    privileged: true
    config:
      platform: linux
      image_resource:
        type: docker-image
        source:
          repository: amidos/dcind
          tag: 2.1.0
      inputs:
      - name: repo
      run:
        path: /bin/sh
        args: 
          - -c
          - |
            source /docker-lib.sh
            start_docker

            cd repo
            docker run -it --rm -v "$PWD:$PWD" -w "$PWD" -v /var/run/docker.sock:/var/run/docker.sock eclipse-temurin:17.0.5_8-jdk-alpine ./mvnw clean package
```

```bash
fly -t tutorial set-pipeline -p testcontainers-pipeline -c pipeline.yml
fly -t tutorial unpause-pipeline -p testcontainers-pipeline
fly -t tutorial trigger-job --job testcontainers-pipeline/testcontainers-job --watch
```
