# CircleCI

## CircleCI Cloud

Testcontainers can be used on CircleCI with either the Docker executor or the Machine executor.

When using the Docker executor, you must include the `setup_remote_docker` step to enable Docker support.

Testcontainers will run containers using the Docker daemon provided by `setup_remote_docker`. Exposed container ports are automatically forwarded to the job container.

Here’s a minimal CircleCI configuration using the Docker executor:


```yaml
version: 2.1
jobs:
  build:
    docker:
      - image: cimg/openjdk:23.0
    steps:
      - checkout
      - setup_remote_docker
      - run: mvn -B clean install
```

!!! warning
    Testcontainers will map exposed container ports to the job container's network. However, due to the remote Docker architecture, **you should not assume containers are accessible via `localhost`**. Always use `.getHost()` and `.getMappedPort()` from Testcontainers to retrieve the correct hostname and port as described [here](/features/networking/#getting-the-container-host).

Alternatively, you can use the Machine executor, which provides native Docker support without `setup_remote_docker`:

```yaml
version: 2.1
jobs:
  build:
    machine:
      image: ubuntu-2204:current
    steps:
      - checkout
      - run: mvn -B clean install
```

## CircleCI Server

On CircleCI Server, `setup_remote_docker` is not supported. To use Testcontainers, you must run your jobs on a dedicated virtual machine using the machine executor. You can configure this by specifying the executor type in your `.circleci/config.yml` to be `machine`. For more information see [Choosing an Executor Type](https://circleci.com/docs/executor-intro/).

Here is a sample CircleCI configuration that does a checkout of a project and runs Maven:

```yaml
jobs:
  build:
    # Check https://circleci.com/docs/executor-intro#linux-vm for more details
    machine: true
    steps:
      - checkout
      - run: mvn -B clean install
```

