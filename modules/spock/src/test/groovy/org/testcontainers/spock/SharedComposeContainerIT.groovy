package org.testcontainers.spock

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
class SharedComposeContainerIT extends Specification {

    @Shared
    DockerComposeContainer composeContainer = new DockerComposeContainer(
            new File("src/test/resources/docker-compose.yml"))
            .withExposedService("whoami_1", 80, Wait.forHttp("/"))

    String host

    int port

    def setup() {
        host = composeContainer.getServiceHost("whoami_1", 80)
        port = composeContainer.getServicePort("whoami_1", 80)
    }

    def "running compose defined container is accessible on configured port"() {
        given: "a http client"
        def client = HttpClientBuilder.create().build()

        when: "accessing web server"
        def response = client.execute(new HttpGet("http://$host:$port"))

        then: "docker container is running and returns http status code 200"
        response.statusLine.statusCode == 200
    }
}
