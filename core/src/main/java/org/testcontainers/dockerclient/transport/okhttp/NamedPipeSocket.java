package org.testcontainers.dockerclient.transport.okhttp;

import com.sun.jna.platform.win32.Kernel32;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnnecessaryInitCause")
public class NamedPipeSocket extends Socket {

    private final String host;
    private final String name;

    private RandomAccessFile file;
    private InputStream is;
    private OutputStream os;

    public NamedPipeSocket(String host, String name) {
        this.host = host;
        this.name = name;
    }

    @Override
    public void close() throws IOException {
        if (file != null) {
            file.close();
            file = null;
        }
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    /**
     * Name pipe connection.
     *
     * @param endpoint endPoint
     * @param timeout  timeout in milliseconds
     * @throws IOException exception
     */
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        String filename;
        if (host == null || host.equals("localhost")) {
            filename = "\\\\.\\pipe\\" + name;
        } else {
            filename = "\\\\" + host + "\\pipe\\" + name;
        }

        //use a default timeout of 100ms if no timeout set.
        int usedTimeout = timeout == 0 ? 100 : timeout;
        long initialNano = System.nanoTime();
        do {
            try {
                file = new RandomAccessFile(filename, "rw");
                break;
            } catch (FileNotFoundException fileNotFoundException) {
                try {
                    //using JNA if available
                    Kernel32.INSTANCE.WaitNamedPipe(filename, timeout);
                    //then retry connection
                    file = new RandomAccessFile(filename, "rw");
                } catch (Throwable cle) {
                    // in case JNA not on classpath, then wait 10ms before next try.
                    if (System.nanoTime() - initialNano > TimeUnit.MILLISECONDS.toNanos(usedTimeout)) {
                        if (timeout == 0) {
                            throw new FileNotFoundException(fileNotFoundException.getMessage()
                                + "\nplease consider set connectTimeout option, so connection can retry having access to named pipe. "
                                + "\n(Named pipe can throw ERROR_PIPE_BUSY error)");
                        }
                        throw fileNotFoundException;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(5);
                    } catch (InterruptedException interrupted) {
                        IOException ioException = new IOException(
                            "Interruption during connection to named pipe");
                        ioException.initCause(interrupted);
                        throw ioException;
                    }
                }
            }
        } while (true);

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

    public InputStream getInputStream() {
        return is;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public void setTcpNoDelay(boolean bool) {
        //do nothing
    }

    public void setKeepAlive(boolean bool) {
        //do nothing
    }

    public void setReceiveBufferSize(int size) {
        //do nothing
    }

    public void setSendBufferSize(int size) {
        //do nothing
    }

    public void setSoLinger(boolean bool, int value) {
        //do nothing
    }

    @Override
    public void setSoTimeout(int timeout) {
        //do nothing
    }

    public void shutdownInput() {
        //do nothing
    }

    public void shutdownOutput() {
        //do nothing
    }
}
