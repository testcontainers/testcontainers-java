package org.testcontainers.spock

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.testcontainers.containers.GenericContainer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
@Testcontainers
class TestcontainersSharedContainerIT extends Specification {

    @Shared
    GenericContainer genericContainer = new GenericContainer(SpockTestImages.HTTPD_IMAGE)
            .withExposedPorts(80)

    @Shared
    String lastContainerId

    def "starts accessible docker container"() {
        given: "a http client"
        def client = HttpClientBuilder.create().build()
        lastContainerId = genericContainer.containerId

        when: "accessing web server"
        CloseableHttpResponse response = performHttpRequest(client)

        then: "docker container is running and returns http status code 200"
        response.statusLine.statusCode == 200
    }

    def "containers keeps on running between features"() {
        expect:
        genericContainer.containerId == lastContainerId
    }

    private CloseableHttpResponse performHttpRequest(CloseableHttpClient client) {
        String ip = genericContainer.host
        String port = genericContainer.getMappedPort(80)
        def response = client.execute(new HttpGet("http://$ip:$port"))
        response
    }

}
