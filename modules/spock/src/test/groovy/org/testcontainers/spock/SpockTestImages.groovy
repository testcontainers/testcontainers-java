package org.testcontainers.spock

import org.testcontainers.utility.DockerImageName

interface SpockTestImages {
    DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:5.7.34")
    DockerImageName POSTGRES_TEST_IMAGE = DockerImageName.parse("postgres:9.6.12")
    DockerImageName HTTPD_IMAGE = DockerImageName.parse("httpd:2.4-alpine")
    DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.5")
}
