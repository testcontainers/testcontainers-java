package org.testcontainers.dockerclient.transport.okhttp;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.scalasbt.ipcsocket.Win32NamedPipeSocket;

import javax.net.SocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Value
@EqualsAndHashCode(callSuper = false)
public class NamedPipeSocketFactory extends SocketFactory {

    String socketPath;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        return new Win32NamedPipeSocket(socketPath.replace("/", "\\")) {

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
