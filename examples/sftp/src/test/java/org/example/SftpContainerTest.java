package org.example;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SftpContainerTest {

    @Test
    void test() throws Exception {
        try (
            GenericContainer<?> sftp = new GenericContainer<>("atmoz/sftp:alpine-3.7")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("testcontainers/", 0777),
                    "/home/foo/upload/testcontainers"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload")
        ) {
            sftp.start();
            JSch jsch = new JSch();
            Session jschSession = jsch.getSession("foo", sftp.getHost(), sftp.getMappedPort(22));
            jschSession.setPassword("pass");
            jschSession.setConfig("StrictHostKeyChecking", "no");
            jschSession.connect();
            ChannelSftp channel = (ChannelSftp) jschSession.openChannel("sftp");
            channel.connect();
            assertThat(channel.ls("/upload/testcontainers")).anyMatch(item -> item.toString().contains("file.txt"));
            assertThat(
                new BufferedReader(
                    new InputStreamReader(channel.get("/upload/testcontainers/file.txt"), StandardCharsets.UTF_8)
                )
                    .lines()
                    .collect(Collectors.joining("\n"))
            )
                .contains("Testcontainers");
            channel.rm("/upload/testcontainers/file.txt");
            assertThat(channel.ls("/upload/testcontainers/"))
                .noneMatch(item -> item.toString().contains("testcontainers/file.txt"));
        }
    }

    @Test
    void testHostKeyCheck() throws Exception {
        try (
            GenericContainer<?> sftp = new GenericContainer<>("atmoz/sftp:alpine-3.7")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("testcontainers/", 0777),
                    "/home/foo/upload/testcontainers"
                )
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("./ssh_host_rsa_key", 0400),
                    "/etc/ssh/ssh_host_rsa_key"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload")
        ) {
            sftp.start();
            JSch jsch = new JSch();
            Session jschSession = jsch.getSession("foo", sftp.getHost(), sftp.getMappedPort(22));
            jschSession.setPassword("pass");
            // hostKeyString is string starting with AAAA from file known_hosts or ssh_host_*_key.pub
            // generate the files with:
            // ssh-keygen -t rsa -b 3072 -f ssh_host_rsa_key < /dev/null
            String hostKeyString =
                "AAAAB3NzaC1yc2EAAAADAQABAAABgQCXMxVRzmFWxfrRB9XiZ/3HNM+xkYYE+IMGuOZD" +
                    "04M2ezU25XjT6cPajzpFmzTxR2qEpRCKHeVnSG5nT6UXQp7760brTN7m5sDasbMnHgYh" +
                    "fC/3of2k6qTR9X/JHRpgwzq5+6FtEe41w1H1dXoNIr4YTKnLijSp8MKqBtPPNUpzEVb9" +
                    "5YKZGdCDoCbbYOyS/Dc8azUDo0mqM542J3nA2Sq9HCP0BAv43hrTAtCZodkB5wo18exb" +
                    "fPKsjGtA3de2npybFoSRbavZmT8L/b2iHZX6FRaqLsbYGKtszCWu5OU7WBX5g5QVlLfO" +
                    "nGQ+LsF6d6pX5LlMwEU14uu4gNPvZFOaZXtHNHZqnBcjd/sMaw5N/atFsPgtQ0vYnrEA" +
                    "D6oDjj0uXMsnmgUWTZBi3q2GBWWPqhE+0ASb2xBQGa+tWWTVYbuuYlA7hUX0URK8FcLw" +
                    "4UOYJjscDjnjlvQkghd2esP5NxV1NXkG2XYNHnf1E/tH4+AHJzy+qOQom7ehda96FZ8=";
            HostKey hostKey = new HostKey(sftp.getHost(), Base64.getDecoder().decode(hostKeyString));
            jschSession.getHostKeyRepository().add(hostKey, null);
            jschSession.connect();
            ChannelSftp channel = (ChannelSftp) jschSession.openChannel("sftp");
            channel.connect();
            assertThat(channel.ls("/upload/testcontainers")).anyMatch(item -> item.toString().contains("file.txt"));
            assertThat(
                new BufferedReader(
                    new InputStreamReader(channel.get("/upload/testcontainers/file.txt"), StandardCharsets.UTF_8)
                )
                    .lines()
                    .collect(Collectors.joining("\n"))
            )
                .contains("Testcontainers");
            channel.rm("/upload/testcontainers/file.txt");
            assertThat(channel.ls("/upload/testcontainers/"))
                .noneMatch(item -> item.toString().contains("testcontainers/file.txt"));
        }
    }
}
