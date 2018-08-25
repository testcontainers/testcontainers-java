package org.testcontainers.dockerclient.transport.okhttp;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.scalasbt.ipcsocket.UnixDomainSocket;

import javax.net.SocketFactory;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Value
@EqualsAndHashCode(callSuper = false)
public class UnixSocketFactory extends SocketFactory {

    String socketPath;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        return new UnixDomainSocket(socketPath) {
            @Override
            public void connect(SocketAddress endpoint, int timeout) throws IOException {
                // Do nothing since it's not "connectable"
            }

            @Override
            public InputStream getInputStream() {
                return new FilterInputStream(super.getInputStream()) {
                    @Override
                    public void close() throws IOException {
                        shutdownInput();
                    }
                };
            }

            @Override
            public OutputStream getOutputStream() {
                return new FilterOutputStream(super.getOutputStream()) {

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        out.write(b, off, len);
                    }

                    @Override
                    public void close() throws IOException {
                        shutdownOutput();
                    }
                };
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
