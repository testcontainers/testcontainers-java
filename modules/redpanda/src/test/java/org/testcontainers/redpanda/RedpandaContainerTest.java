package org.testcontainers.redpanda;

import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RedpandaContainerTest extends AbstractRedpanda {

    private static final String REDPANDA_IMAGE = "docker.redpanda.com/redpandadata/redpanda:v22.2.1";

    private static final DockerImageName REDPANDA_DOCKER_IMAGE = DockerImageName.parse(REDPANDA_IMAGE);

    @Test
    public void testUsage() throws Exception {
        try (RedpandaContainer container = new RedpandaContainer(REDPANDA_DOCKER_IMAGE)) {
            container.start();
            testKafkaFunctionality(container.getBootstrapServers());
        }
    }

    @Test
    public void testUsageWithStringImage() throws Exception {
        try (
            // constructorWithVersion {
            RedpandaContainer container = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.2")
            // }
        ) {
            container.start();
            testKafkaFunctionality(
                // getBootstrapServers {
                container.getBootstrapServers()
                // }
            );
        }
    }

    @Test
    public void testNotCompatibleVersion() {
        assertThatThrownBy(() -> new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v21.11.19"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Redpanda version must be >= v22.2.1");
    }

    @Test
    public void vectorizedRedpandaImageVersion2221ShouldNotBeCompatible() {
        assertThatThrownBy(() -> new RedpandaContainer("docker.redpanda.com/vectorized/redpanda:v21.11.19"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Redpanda version must be >= v22.2.1");
    }

    @Test
    public void redpandadataRedpandaImageVersion2221ShouldNotBeCompatible() {
        assertThatThrownBy(() -> new RedpandaContainer("redpandadata/redpanda:v21.11.19"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Redpanda version must be >= v22.2.1");
    }

    @Test
    public void testSchemaRegistry() {
        try (RedpandaContainer container = new RedpandaContainer(REDPANDA_DOCKER_IMAGE)) {
            container.start();

            String subjectsEndpoint = String.format(
                "%s/subjects",
                // getSchemaRegistryAddress {
                container.getSchemaRegistryAddress()
                // }
            );

            String subjectName = String.format("test-%s-value", UUID.randomUUID());

            Response createSubject = RestAssured
                .given()
                .contentType("application/vnd.schemaregistry.v1+json")
                .pathParam("subject", subjectName)
                .body("{\"schema\": \"{\\\"type\\\": \\\"string\\\"}\"}")
                .when()
                .post(subjectsEndpoint + "/{subject}/versions")
                .thenReturn();
            assertThat(createSubject.getStatusCode()).isEqualTo(200);

            Response allSubjects = RestAssured.given().when().get(subjectsEndpoint).thenReturn();
            assertThat(allSubjects.getStatusCode()).isEqualTo(200);
            assertThat(allSubjects.jsonPath().getList("$")).contains(subjectName);
        }
    }

    @Test
    public void testUsageWithListener() throws Exception {
        try (
            Network network = Network.newNetwork();
            // registerListener {
            RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")
                .withListener(() -> "redpanda:19092")
                .withNetwork(network);
            // }
            // createKCatContainer {
            GenericContainer<?> kcat = new GenericContainer<>("confluentinc/cp-kcat:7.4.1")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sh");
                })
                .withCopyToContainer(Transferable.of("Message produced by kcat"), "/data/msgs.txt")
                .withNetwork(network)
                .withCommand("-c", "tail -f /dev/null")
            // }
        ) {
            redpanda.start();
            kcat.start();
            // produceConsumeMessage {
            kcat.execInContainer("kcat", "-b", "redpanda:19092", "-t", "msgs", "-P", "-l", "/data/msgs.txt");
            String stdout = kcat
                .execInContainer("kcat", "-b", "redpanda:19092", "-C", "-t", "msgs", "-c", "1")
                .getStdout();
            // }
            assertThat(stdout).contains("Message produced by kcat");
        }
    }

    @Test
    public void testUsageWithListenerAndSasl() throws Exception {
        final String username = "panda";
        final String password = "pandapass";
        final String algorithm = "SCRAM-SHA-256";

        try (
            Network network = Network.newNetwork();
            RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")
                .enableAuthorization()
                .enableSasl()
                .withSuperuser("panda")
                .withListener(() -> "my-panda:29092")
                .withNetwork(network);
            GenericContainer<?> kcat = new GenericContainer<>("confluentinc/cp-kcat:7.4.1")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sh");
                })
                .withCopyToContainer(Transferable.of("Message produced by kcat"), "/data/msgs.txt")
                .withNetwork(network)
                .withCommand("-c", "tail -f /dev/null")
        ) {
            redpanda.start();

            String adminUrl = String.format("%s/v1/security/users", redpanda.getAdminAddress());
            Map<String, String> params = new HashMap<>();
            params.put("username", username);
            params.put("password", password);
            params.put("algorithm", algorithm);

            RestAssured.given().contentType("application/json").body(params).post(adminUrl).then().statusCode(200);

            kcat.start();

            kcat.execInContainer(
                "kcat",
                "-b",
                "my-panda:29092",
                "-X",
                "security.protocol=SASL_PLAINTEXT",
                "-X",
                "sasl.mechanisms=" + algorithm,
                "-X",
                "sasl.username=" + username,
                "-X",
                "sasl.password=" + password,
                "-t",
                "msgs",
                "-P",
                "-l",
                "/data/msgs.txt"
            );

            String stdout = kcat
                .execInContainer(
                    "kcat",
                    "-b",
                    "my-panda:29092",
                    "-X",
                    "security.protocol=SASL_PLAINTEXT",
                    "-X",
                    "sasl.mechanisms=" + algorithm,
                    "-X",
                    "sasl.username=" + username,
                    "-X",
                    "sasl.password=" + password,
                    "-C",
                    "-t",
                    "msgs",
                    "-c",
                    "1"
                )
                .getStdout();

            assertThat(stdout).contains("Message produced by kcat");
        }
    }

    @SneakyThrows
    @Test
    public void enableSaslWithSuccessfulTopicCreation() {
        try (
            // security {
            RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")
                .enableAuthorization()
                .enableSasl()
                .withSuperuser("superuser-1")
            // }
        ) {
            redpanda.start();

            createSuperUser(redpanda);

            AdminClient adminClient = getAdminClient(redpanda);
            String topicName = "messages-" + UUID.randomUUID();
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, 1, (short) 1));
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

            assertThat(adminClient.listTopics().names().get()).contains(topicName);
        }
    }

    @Test
    public void enableSaslWithUnsuccessfulTopicCreation() {
        try (
            RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")
                .enableAuthorization()
                .enableSasl()
        ) {
            redpanda.start();

            createSuperUser(redpanda);

            AdminClient adminClient = getAdminClient(redpanda);
            String topicName = "messages-" + UUID.randomUUID();
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, 1, (short) 1));

            Awaitility
                .await()
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(TopicAuthorizationException.class);
                });
        }
    }

    @Test
    public void enableSaslAndWithAuthenticationError() {
        try (
            RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")
                .enableAuthorization()
                .enableSasl()
        ) {
            redpanda.start();

            AdminClient adminClient = getAdminClient(redpanda);
            String topicName = "messages-" + UUID.randomUUID();
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, 1, (short) 1));

            Awaitility
                .await()
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(SaslAuthenticationException.class);
                });
        }
    }

    @Test
    public void schemaRegistryWithHttpBasic() {
        try (
            RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")
                .enableSchemaRegistryHttpBasicAuth()
                .withSuperuser("superuser-1")
        ) {
            redpanda.start();

            createSuperUser(redpanda);

            String subjectsEndpoint = String.format("%s/subjects", redpanda.getSchemaRegistryAddress());

            RestAssured.when().get(subjectsEndpoint).then().statusCode(401);

            RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("superuser-1", "test")
                .get(subjectsEndpoint)
                .then()
                .statusCode(200);
        }
    }

    @SneakyThrows
    @Test
    public void testRestProxy() {
        try (RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.7")) {
            redpanda.start();

            redpanda.execInContainer("rpk", "topic", "create", "test_topic", "-p", "3");

            String applicationKafkaJson = "application/vnd.kafka.json.v2+json";

            String restProxy = redpanda.getRestProxyAddress();
            RestAssured
                .given()
                .contentType(applicationKafkaJson)
                .body(
                    "{\"records\":[{\"value\":\"jsmith\",\"partition\":0},{\"value\":\"htanaka\",\"partition\":1},{\"value\":\"awalther\",\"partition\":2}]}"
                )
                .post(String.format("%s/topics/test_topic", restProxy))
                .then()
                .statusCode(200);

            RestAssured
                .given()
                .contentType("application/vnd.kafka.v2+json")
                .body("{\"name\": \"test_consumer\", \"format\": \"json\", \"auto.offset.reset\": \"earliest\"}")
                .post(String.format("%s/consumers/test_group", restProxy))
                .then()
                .statusCode(200);
            RestAssured
                .given()
                .contentType("application/vnd.kafka.v2+json")
                .body("{\"topics\":[\"test_topic\"]}")
                .post(String.format("%s/consumers/test_group/instances/test_consumer/subscription", restProxy))
                .then()
                .statusCode(204);

            List<Map<String, String>> response = RestAssured
                .given()
                .accept(applicationKafkaJson)
                .get(String.format("%s/consumers/test_group/instances/test_consumer/records", restProxy))
                .getBody()
                .as(new TypeRef<List<Map<String, String>>>() {});
            assertThat(response).hasSize(3).extracting("value").containsExactly("jsmith", "htanaka", "awalther");
        }
    }

    private AdminClient getAdminClient(RedpandaContainer redpanda) {
        String bootstrapServer = String.format("%s:%s", redpanda.getHost(), redpanda.getMappedPort(9092));
        // createAdminClient {
        AdminClient adminClient = AdminClient.create(
            ImmutableMap.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServer,
                AdminClientConfig.SECURITY_PROTOCOL_CONFIG,
                "SASL_PLAINTEXT",
                SaslConfigs.SASL_MECHANISM,
                "SCRAM-SHA-256",
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"superuser-1\" password=\"test\";"
            )
        );
        // }
        return adminClient;
    }

    private void createSuperUser(RedpandaContainer redpanda) {
        String adminUrl = String.format("%s/v1/security/users", redpanda.getAdminAddress());
        RestAssured
            .given()
            .contentType("application/json")
            .body("{\"username\": \"superuser-1\", \"password\": \"test\", \"algorithm\": \"SCRAM-SHA-256\"}")
            .post(adminUrl)
            .then()
            .statusCode(200);
    }
}
