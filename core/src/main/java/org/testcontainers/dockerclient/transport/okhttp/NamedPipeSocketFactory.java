package org.testcontainers.dockerclient.transport.okhttp;

import de.gesellix.docker.client.filesocket.FileSocket;
import de.gesellix.docker.client.filesocket.HostnameEncoder;
import de.gesellix.docker.client.filesocket.NamedPipeSocket;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Value
@EqualsAndHashCode(callSuper = false)
public class NamedPipeSocketFactory extends SocketFactory {

    String socketPath;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        return new NamedPipeSocket() {
            @Override
            public void connect(SocketAddress endpoint, int timeout) throws IOException {
                super.connect(
                    new InetSocketAddress(
                        InetAddress.getByAddress(encodeHostname(socketPath), new byte[] { 0, 0, 0, 0}),
                        0
                    ),
                    timeout
                );
            }
        };
    }

    @Override
    public Socket createSocket(String s, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) {
        throw new UnsupportedOperationException();
    }
}
