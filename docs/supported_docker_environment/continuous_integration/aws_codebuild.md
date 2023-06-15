# AWS CodeBuild

To enable access to Docker in AWS CodeBuild, go to `Privileged` section and check  
`Enable this flag if you want to build Docker images or want your builds to get elevated privileges`.

This is a sample `buildspec.yml` config:

```yaml
version: 0.2

phases:
  install:
    runtime-versions:
      java: corretto17
  build:
    commands:
      - ./mvnw test
```
