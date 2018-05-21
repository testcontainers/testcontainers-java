package org.testcontainers.dockerclient.transport.okhttp;

import lombok.SneakyThrows;
import lombok.Value;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import javax.net.SocketFactory;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;

@Value
public class UnixSocketFactory extends SocketFactory {

    String socketPath;

    @Override
    @SneakyThrows
    public Socket createSocket() {
        AFUNIXSocket socket = AFUNIXSocket.connectTo(new AFUNIXSocketAddress(new File(socketPath)));

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Socket.class);
        enhancer.setCallback((InvocationHandler) (proxy, method, args) -> {
            if ("connect".equals(method.getName())) {
                return null;
            }

            return method.invoke(socket, args);
        });

        return (Socket) enhancer.create();
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
