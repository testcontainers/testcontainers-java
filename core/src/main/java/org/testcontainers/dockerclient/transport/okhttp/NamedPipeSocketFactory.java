package org.testcontainers.dockerclient.transport.okhttp;

import com.sun.jna.platform.win32.Kernel32;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.rnorth.ducttape.unreliables.Unreliables;

import javax.net.SocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@Value
@EqualsAndHashCode(callSuper = false)
public class NamedPipeSocketFactory extends SocketFactory {

    String socketFileName;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        return new Socket() {

            RandomAccessFile file;
            InputStream is;
            OutputStream os;

            @Override
            public void close() throws IOException {
                if (file != null) {
                    file.close();
                    file = null;
                }
            }

            @Override
            public void connect(SocketAddress endpoint) {
                connect(endpoint, 0);
            }

            @Override
            public void connect(SocketAddress endpoint, int timeout) {
                file = Unreliables.retryUntilSuccess(Math.max(timeout, 10_000), TimeUnit.MILLISECONDS, () -> {
                    try {
                        return new RandomAccessFile(socketFileName, "rw");
                    } catch (FileNotFoundException e) {
                        Kernel32.INSTANCE.WaitNamedPipe(socketFileName, 100);
                        throw e;
                    }
                });

                is = new InputStream() {
                    @Override
                    public int read(byte[] bytes, int off, int len) throws IOException {
                        return file.read(bytes, off, len);
                    }

                    @Override
                    public int read() throws IOException {
                        return file.read();
                    }

                    @Override
                    public int read(byte[] bytes) throws IOException {
                        return file.read(bytes);
                    }
                };

                os = new OutputStream() {
                    @Override
                    public void write(byte[] bytes, int off, int len) throws IOException {
                        file.write(bytes, off, len);
                    }

                    @Override
                    public void write(int value) throws IOException {
                        file.write(value);
                    }

                    @Override
                    public void write(byte[] bytes) throws IOException {
                        file.write(bytes);
                    }
                };
            }

            @Override
            public InputStream getInputStream() {
                return is;
            }

            @Override
            public OutputStream getOutputStream() {
                return os;
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
