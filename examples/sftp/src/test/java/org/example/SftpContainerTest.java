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
            GenericContainer<?> sftp = new GenericContainer<>("jmcombs/sftp:alpine")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("testcontainers/", 0777),
                    "/home/foo/upload/testcontainers"
                )
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("./ssh_host_ed25519_key", 0400),
                    "/etc/ssh/ssh_host_ed25519_key"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload")
        ) {
            sftp.start();
            JSch jsch = new JSch();
            Session jschSession = jsch.getSession("foo", sftp.getHost(), sftp.getMappedPort(22));
            jschSession.setPassword("pass");
            // hostKeyString is string starting with AAAA from file known_hosts or ssh_host_*_key.pub
            String hostKeyString = "AAAAC3NzaC1lZDI1NTE5AAAAINaBuegbLGHOgpXCePq80uY79Xw716jWXAwWjRdFYi53";
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
