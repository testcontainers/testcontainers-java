package org.testcontainers;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;

import java.nio.file.Paths;

import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * Created by rnorth on 09/08/2015.
 */
public class SingletonDockerClient {

    private static SingletonDockerClient instance;
    private final DefaultDockerClient client;
    private String dockerHostIpAddress;

    private SingletonDockerClient() {
        try {
            client = customizeBuilderForOs().build();

            String version = client.version().version();
            checkVersion(version);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Docker client", e);
        }
    }

    public synchronized static SingletonDockerClient instance() {
        if (instance == null) {
            instance = new SingletonDockerClient();
        }

        return instance;
    }

    public DockerClient client() {
        return client;
    }

    public String dockerHostIpAddress() {
        return dockerHostIpAddress;
    }

    private DefaultDockerClient.Builder customizeBuilderForOs() throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {

            DefaultDockerClient.Builder builder = DefaultDockerClient.builder();

            // Running on a Mac therefore use boot2docker
            runShellCommand("/usr/local/bin/boot2docker", "up");
            dockerHostIpAddress = runShellCommand("/usr/local/bin/boot2docker", "ip");

            builder.uri("https://" + dockerHostIpAddress + ":2376")
                    .dockerCertificates(new DockerCertificates(Paths.get(System.getProperty("user.home") + "/.boot2docker/certs/boot2docker-vm")));

            return builder;
        } else {
            dockerHostIpAddress = "127.0.0.1";
            return DefaultDockerClient.fromEnv();
        }
    }

    private void checkVersion(String version) {
        String[] splitVersion = version.split("\\.");
        if (Integer.valueOf(splitVersion[0]) <= 1 && Integer.valueOf(splitVersion[1]) < 6) {
            throw new IllegalStateException("Docker version 1.6.0+ is required, but version " + version + " was found");
        }
    }
}
