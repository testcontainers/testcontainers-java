package org.testcontainers.dockerclient.transport.okhttp;

import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import lombok.SneakyThrows;
import lombok.Value;

import javax.net.SocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Value
public class UnixSocketFactory extends SocketFactory {

    String socketPath;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        return new UnixSocket(UnixSocketChannel.open()) {

            @Override
            public void connect(SocketAddress addr, Integer timeout) throws IOException {
                addr = new UnixSocketAddress(socketPath);
                super.connect(addr, timeout);
            }

            @Override
            public void connect(SocketAddress endpoint, int timeout) throws IOException {
                connect(endpoint, new Integer(timeout));
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FilterInputStream(super.getInputStream()) {
                    @Override
                    public void close() throws IOException {
                        shutdownInput();
                    }
                };
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
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
