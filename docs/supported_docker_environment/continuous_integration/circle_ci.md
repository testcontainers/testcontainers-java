# CircleCI 2.0

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
