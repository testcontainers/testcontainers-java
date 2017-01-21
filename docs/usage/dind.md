# Docker in Docker

Since TestContainers version 1.1.8, you can run your tests inside a container.
It's very useful for different CI scenarios like running everything in containers on Jenkins.

TestContainers will automatically detect if it's inside a container and instead of "localhost" will use the default gateway's IP.

However, an additional configuration is required if you use [volume mapping](options.md#volume-mapping).

## Docker-only
If you run the tests with just `docker run ...` then make sure you add `-v $PWD:$PWD -w $PWD -v /var/run/docker.sock:/var/run/docker.sock` to the command, so it will look like this:
```bash
$ tree .
.
├── pom.xml
└── src
    └── test
        └── java
            └── MyTestWithTestContainers.java

$ docker run -it --rm -v $PWD:$PWD -w $PWD -v /var/run/docker.sock:/var/run/docker.sock maven:3 mvn test
```

Where:
* `-v $PWD:$PWD` will add your current directory as a volume inside the container
* `-w $PWD` will set the current directory to this volume
* `-v /var/run/docker.sock:/var/run/docker.sock` will map the Docker socket

## Docker Compose
The same can be achived with Docker Compose:
```yaml
tests:
  image: maven:3
  stop_signal: SIGKILL
  stdin_open: true
  tty: true
  working_dir: $PWD
  volumes:
    - $PWD:$PWD
    - /var/run/docker.sock:/var/run/docker.sock
    # Maven cache (optional)
    - ~/.m2:/root/.m2
  command: mvn test
```
