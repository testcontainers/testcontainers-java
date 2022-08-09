package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface DynamoDBTestImages {

    DockerImageName AWS_DYNAMODB_IMAGE = DockerImageName.parse("amazon/dynamodb-local:1.18.0");
}
