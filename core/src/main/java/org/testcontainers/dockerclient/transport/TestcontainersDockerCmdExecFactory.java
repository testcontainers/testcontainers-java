package org.testcontainers.dockerclient.transport;

import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.AbstractDockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.SSLConfig;
import com.github.dockerjava.core.WebTarget;
import com.github.dockerjava.netty.NettyWebTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DuplexChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang.SystemUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testcontainers.DockerClientFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.Security;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * This class is a modified version of docker-java's NettyDockerCmdExecFactory v3.1.0-rc-2
 * Changes:
 * - daemonized thread factory
 * - thread group
 * - the logging handler removed
 * -
 */
public class TestcontainersDockerCmdExecFactory extends AbstractDockerCmdExecFactory implements DockerCmdExecFactory {

    private static final String THREAD_PREFIX = "testcontainers-netty";

    /*
     * useful links:
     *
     * http://stackoverflow.com/questions/33296749/netty-connect-to-unix-domain-socket-failed
     * http://netty.io/wiki/native-transports.html
     * https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/snoop/HttpSnoopClient.java
     * https://github.com/slandelle/netty-request-chunking/blob/master/src/test/java/slandelle/ChunkingTest.java
     */

    private Bootstrap bootstrap;

    private EventLoopGroup eventLoopGroup;

    private NettyInitializer nettyInitializer;

    private WebTarget baseResource;

    private Integer connectTimeout = null;

    private Integer readTimeout = null;

    @Override
    public void init(DockerClientConfig dockerClientConfig) {
        super.init(dockerClientConfig);

        bootstrap = new Bootstrap();

        String scheme = dockerClientConfig.getDockerHost().getScheme();

        if ("unix".equals(scheme)) {
            nettyInitializer = new UnixDomainSocketInitializer();
        } else if ("tcp".equals(scheme)) {
            nettyInitializer = new InetSocketInitializer();
        }

        eventLoopGroup = nettyInitializer.init(bootstrap, dockerClientConfig);

        baseResource = new NettyWebTarget(this::connect).path(dockerClientConfig.getApiVersion().asWebPathPart());
    }

    private DuplexChannel connect() {
        checkState(!eventLoopGroup.isShuttingDown(), "EventLoop is shutting down");

        try {
            return connect(bootstrap);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private DuplexChannel connect(final Bootstrap bootstrap) throws InterruptedException {
        return nettyInitializer.connect(bootstrap);
    }

    private interface NettyInitializer {
        EventLoopGroup init(final Bootstrap bootstrap, DockerClientConfig dockerClientConfig);

        DuplexChannel connect(final Bootstrap bootstrap) throws InterruptedException;
    }

    private ThreadFactory createThreadFactory() {
        return new DefaultThreadFactory(THREAD_PREFIX, true, Thread.NORM_PRIORITY, DockerClientFactory.TESTCONTAINERS_THREAD_GROUP);
    }

    private class UnixDomainSocketInitializer implements NettyInitializer {
        @Override
        public EventLoopGroup init(Bootstrap bootstrap, DockerClientConfig dockerClientConfig) {
            if (SystemUtils.IS_OS_LINUX) {
                return epollGroup();
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                return kqueueGroup();
            }
            throw new RuntimeException("Unspported OS");
        }

        public EventLoopGroup epollGroup() {
            EventLoopGroup epollEventLoopGroup = new EpollEventLoopGroup(0, createThreadFactory());

            ChannelFactory<EpollDomainSocketChannel> factory = () -> configure(new EpollDomainSocketChannel());

            bootstrap.group(epollEventLoopGroup).channelFactory(factory).handler(new ChannelInitializer<UnixChannel>() {
                @Override
                protected void initChannel(final UnixChannel channel) throws Exception {
                    channel.pipeline().addLast(new HttpClientCodec());
                }
            });
            return epollEventLoopGroup;
        }

        public EventLoopGroup kqueueGroup() {
            EventLoopGroup nioEventLoopGroup = new KQueueEventLoopGroup(0, createThreadFactory());

            bootstrap.group(nioEventLoopGroup).channel(KQueueDomainSocketChannel.class)
                    .handler(new ChannelInitializer<KQueueDomainSocketChannel>() {
                        @Override
                        protected void initChannel(final KQueueDomainSocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new HttpClientCodec());
                        }
                    });

            return nioEventLoopGroup;
        }

        @Override
        public DuplexChannel connect(Bootstrap bootstrap) throws InterruptedException {
            return (DuplexChannel) bootstrap.connect(new DomainSocketAddress("/var/run/docker.sock")).sync().channel();
        }
    }

    private class InetSocketInitializer implements NettyInitializer {
        @Override
        public EventLoopGroup init(Bootstrap bootstrap, final DockerClientConfig dockerClientConfig) {
            EventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(0, createThreadFactory());

            // TODO do we really need BouncyCastle?
            Security.addProvider(new BouncyCastleProvider());

            ChannelFactory<NioSocketChannel> factory = () -> configure(new NioSocketChannel());

            bootstrap.group(nioEventLoopGroup).channelFactory(factory)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(final SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new HttpClientCodec());
                        }
                    });

            return nioEventLoopGroup;
        }

        @Override
        public DuplexChannel connect(Bootstrap bootstrap) throws InterruptedException {
            DockerClientConfig dockerClientConfig = getDockerClientConfig();
            String host = dockerClientConfig.getDockerHost().getHost();
            int port = dockerClientConfig.getDockerHost().getPort();

            if (port == -1) {
                throw new RuntimeException("no port configured for " + host);
            }

            DuplexChannel channel = (DuplexChannel) bootstrap.connect(host, port).sync().channel();

            final SslHandler ssl = initSsl(dockerClientConfig);

            if (ssl != null) {
                channel.pipeline().addFirst(ssl);
            }

            return channel;
        }

        private SslHandler initSsl(DockerClientConfig dockerClientConfig) {
            SslHandler ssl = null;

            try {
                String host = dockerClientConfig.getDockerHost().getHost();
                int port = dockerClientConfig.getDockerHost().getPort();

                final SSLConfig sslConfig = dockerClientConfig.getSSLConfig();

                if (sslConfig != null && sslConfig.getSSLContext() != null) {

                    SSLEngine engine = sslConfig.getSSLContext().createSSLEngine(host, port);
                    engine.setUseClientMode(true);
                    engine.setSSLParameters(enableHostNameVerification(engine.getSSLParameters()));

                    // in the future we may use HostnameVerifier like here:
                    // https://github.com/AsyncHttpClient/async-http-client/blob/1.8.x/src/main/java/com/ning/http/client/providers/netty/NettyConnectListener.java#L76

                    ssl = new SslHandler(engine);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return ssl;
        }
    }

    public SSLParameters enableHostNameVerification(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        return sslParameters;
    }

    @Override
    public void close() throws IOException {
        checkNotNull(eventLoopGroup, "Factory not initialized. You probably forgot to call init()!");

        eventLoopGroup.shutdownGracefully();
    }

    /**
     * Configure connection timeout in milliseconds
     */
    public TestcontainersDockerCmdExecFactory withConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Configure read timeout in milliseconds
     */
    public TestcontainersDockerCmdExecFactory withReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    private <T extends Channel> T configure(T channel) {
        ChannelConfig channelConfig = channel.config();

        if (connectTimeout != null) {
            channelConfig.setConnectTimeoutMillis(connectTimeout);
        }
        if (readTimeout != null) {
            channel.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler());
        }

        return channel;
    }

    private final class ReadTimeoutHandler extends IdleStateHandler {
        private boolean alreadyTimedOut;

        ReadTimeoutHandler() {
            super(readTimeout, 0, 0, TimeUnit.MILLISECONDS);
        }

        /**
         * Called when a read timeout was detected.
         */
        @Override
        protected synchronized void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
            assert evt.state() == IdleState.READER_IDLE;
            final Channel channel = ctx.channel();
            if (channel == null || !channel.isActive() || alreadyTimedOut) {
                return;
            }
            DockerClientConfig dockerClientConfig = getDockerClientConfig();
            final Object dockerAPIEndpoint = dockerClientConfig.getDockerHost();
            final String msg = "Read timed out: No data received within " + readTimeout
                    + "ms.  Perhaps the docker API (" + dockerAPIEndpoint
                    + ") is not responding normally, or perhaps you need to increase the readTimeout value.";
            final Exception ex = new SocketTimeoutException(msg);
            ctx.fireExceptionCaught(ex);
            alreadyTimedOut = true;
        }
    }

    protected WebTarget getBaseResource() {
        checkNotNull(baseResource, "Factory not initialized, baseResource not set. You probably forgot to call init()!");
        return baseResource;
    }
}
