# Generic containers

A generic container rule can be used with any public docker image; for example:

    // Set up a redis container
    @ClassRule
    public static GenericContainerRule redis = new GenericContainerRule("redis:3.0.2")
                                            .withExposedPorts(6379);


    // Set up a plain OS container and customize environment, command and exposed ports. This just listens on port 80 and always returns '42'
    @ClassRule
    public static GenericContainerRule alpine = new GenericContainerRule("alpine:3.2")
                                                   .withExposedPorts(80)
                                                   .withEnv("MAGIC_NUMBER", "42")
                                                   .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");
