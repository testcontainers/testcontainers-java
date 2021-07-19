# CircleCI 2.0

## Machine executor
Your CircleCI configuration should use a dedicated VM for testcontainers to work. You can achieve this by specifying the 
executor type in your `.circleci/config.yml` to be `machine` instead of the default `docker` executor ( see [Choosing an Executor Type](https://circleci.com/docs/2.0/executor-types/) for more info ).  

Here is a sample CircleCI configuration that does a checkout of a project and runs maven:

```yml
# Check https://circleci.com/docs/2.0/language-java/ for more details
#
version: 2
machine: true
jobs:
  build:
    steps:
      - checkout

      - run: mvn -B clean install
```


## Docker executor
Alternatively you can use testcontainers with a `docker` executor, but there are limitations due to the fact
that this will be a remote docker environment which is firewalled and only reachable through SSH.

Conceptually you need to follow these steps:

- Add `setup-remote-docker` to `.circleci/config.yml`
- Add a login step if you need private container images during test.
- Set the environment variable `TESTCONTAINERS_HOST_OVERRIDE=localhost`. Ports are mapped to localhost through SSH.
- Create tunnels into the remote docker for every exposed port. 
  The reason is that the remote docker is firewalled and only available through 
  `ssh remote-docker`. In the example below `.circleci/autoforward.py` runs in the background, monitors docker port
  mappings and creates SSH port forwards to localhost on the fly.

A sample config `.circleci/config.yml`

```yml
version: 2.1
jobs:
    test:
        docker:
            # choose an image that has: 
            #    ssh, java, git, docker-cli, tar, gzip, python3
            - image: cimg/openjdk:16.0.0
        steps:
            - checkout
            - setup_remote_docker:
                  version: 20.10.2
                  docker_layer_caching: true
            - run:
                  name: Docker login
                  command: |
                      # access private container images during tests
                      echo ${DOCKER_PASS} | \
                        docker login ${DOCKER_REGISTRY_URL} \
                          -u ${DOCKER_USER} \ 
                          --password-stdin
            - run:
                  name: Setup Environment Variables
                  command: |
                      echo "export TESTCONTAINERS_HOST_OVERRIDE=localhost" \
                        >> $BASH_ENV
            - run:
                  name: Testcontainers tunnel
                  background: true
                  command: .circleci/autoforward.py
            - run: ./gradlew clean test --stacktrace
workflows:
    test:
        jobs:
            - test
```
And the script handling the port forwards: [.circleci/autoforward.py](circleci/autoforward.py)

