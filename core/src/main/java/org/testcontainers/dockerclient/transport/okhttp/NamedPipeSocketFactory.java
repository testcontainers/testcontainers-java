package org.testcontainers.dockerclient.transport.okhttp;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.mariadb.jdbc.internal.io.socket.NamedPipeSocket;

import javax.net.SocketFactory;
import java.net.InetAddress;
import java.net.Socket;

@Value
@EqualsAndHashCode(callSuper = false)
public class NamedPipeSocketFactory extends SocketFactory {

    String socketPath;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        String pipeName = socketPath.substring("//./pipe/".length()).replace("/", "\\");
        return new NamedPipeSocket("localhost", pipeName);
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
