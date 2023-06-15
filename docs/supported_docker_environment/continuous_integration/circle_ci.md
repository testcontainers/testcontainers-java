# CircleCI (Cloud, Server v2.x, and Server v3.x)

Your CircleCI configuration should use a dedicated VM for Testcontainers to work. You can achieve this by specifying the 
executor type in your `.circleci/config.yml` to be `machine` instead of the default `docker` executor (see [Choosing an Executor Type](https://circleci.com/docs/2.0/executor-types/) for more info).  

Here is a sample CircleCI configuration that does a checkout of a project and runs Maven:

```yml
jobs:
  build:
    # Check https://circleci.com/docs/executor-intro#linux-vm for more details
    machine: true
    steps:
      - checkout
      - run: mvn -B clean install
```

You can learn more about the best practices of using Testcontainers together with CircleCI in [this article](https://www.atomicjar.com/2022/12/testcontainers-with-circleci/).
