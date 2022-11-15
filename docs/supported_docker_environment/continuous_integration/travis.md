# Travis

To run Testcontainers on TravisCI, docker needs to be installed. The configuration below
is the minimal required config.

```yaml
language: java
jdk:
- openjdk8

services:
- docker

script: ./mvnw verify
```
