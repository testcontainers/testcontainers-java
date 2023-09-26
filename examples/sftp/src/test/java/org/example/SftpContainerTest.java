package org.example;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
}
