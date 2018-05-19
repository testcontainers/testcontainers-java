package org.testcontainers.dockerclient.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.exception.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.AbstractDockerCmdExecFactory;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.WebTarget;
import com.github.dockerjava.core.exec.PingCmdExec;
import com.github.dockerjava.netty.handler.FramedResponseStreamHandler;
import com.github.dockerjava.netty.handler.JsonResponseCallbackHandler;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import de.gesellix.docker.client.filesocket.UnixSocket;
import de.gesellix.docker.client.filesocket.UnixSocketFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import okhttp3.*;
import okhttp3.internal.connection.RealConnection;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.DockerClientFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class OkHttpDockerCmdExecFactory extends AbstractDockerCmdExecFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected WebTarget getBaseResource() {
        UnixSocketFactory unixSocketFactory = new UnixSocketFactory();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .socketFactory(unixSocketFactory)
            .dns(unixSocketFactory)
            .build();

        return new OkHttpWebTarget(
            getDockerClientConfig().getDockerHost(),
            okHttpClient,
            ImmutableList.of(),
            MultimapBuilder.hashKeys().hashSetValues().build()
        );
    }

    @Override
    public PingCmd.Exec createPingCmdExec() {
        return new PingCmdExec(getBaseResource(), getDockerClientConfig()) {

            @Override
            protected Void execute(PingCmd command) {
                WebTarget webResource = getBaseResource().path("/_ping");

                // TODO contribute to docker-java, make it close the stream
                IOUtils.closeQuietly(webResource.request().get());

                return null;
            }
        };
    }

    @Override
    public void close() throws IOException {

    }

    @Wither
    @Value
    private static class OkHttpWebTarget implements WebTarget {

        URI dockerHost;

        OkHttpClient okHttpClient;

        ImmutableList<String> path;

        SetMultimap<String, String> queryParams;

        @Override
        public InvocationBuilder request() {
            String resource = StringUtils.join(path, "/");

            if (!resource.startsWith("/")) {
                resource = "/" + resource;
            }

            HttpUrl.Builder urlBuilder = new HttpUrl.Builder();

            switch (dockerHost.getScheme()) {
                case "unix":
                    urlBuilder
                        .scheme("http")
                        .host(new UnixSocket().encodeHostname(dockerHost.getPath()))
                        .encodedPath(resource);
                    break;
                default:
                    throw new IllegalStateException("Unknown scheme, URI: " + dockerHost);
            }

            for (Map.Entry<String, Collection<String>> queryParamEntry : queryParams.asMap().entrySet()) {
                String key = queryParamEntry.getKey();
                for (String paramValue : queryParamEntry.getValue()) {
                    urlBuilder.addQueryParameter(key, paramValue);
                }
            }

            HttpUrl url = urlBuilder.build();
            return new OkHttpInvocationBuilder(okHttpClient, url, Collections.emptyMap());
        }

        @Override
        public OkHttpWebTarget path(String... components) {
            return this.withPath(
                ImmutableList.<String>builder()
                    .addAll(path)
                    .add(components)
                    .build()
            );
        }

        @Override
        public OkHttpWebTarget resolveTemplate(String name, Object value) {
            ImmutableList.Builder<String> newPath = ImmutableList.builder();
            for (String component : path) {
                component = component.replaceAll("\\{" + name + "\\}", value.toString());
                newPath.add(component);
            }
            return this.withPath(newPath.build());
        }

        @Override
        public OkHttpWebTarget queryParam(String name, Object value) {
            if (value == null) {
                return this;
            }

            SetMultimap<String, String> newQueryParams = HashMultimap.create(queryParams);
            newQueryParams.put(name, value.toString());

            return this.withQueryParams(newQueryParams);
        }

        @Override
        public OkHttpWebTarget queryParamsSet(String name, Set<?> values) {
            SetMultimap<String, String> newQueryParams = HashMultimap.create(queryParams);
            newQueryParams.replaceValues(name, values.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet()));

            return this.withQueryParams(newQueryParams);
        }

        @Override
        @SneakyThrows(JsonProcessingException.class)
        public OkHttpWebTarget queryParamsJsonMap(String name, Map<String, String> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }

            // when param value is JSON string
            return queryParam(name, MAPPER.writeValueAsString(values));
        }

        @Value
        @Wither
        private static class OkHttpInvocationBuilder implements InvocationBuilder {

            OkHttpClient okHttpClient;

            HttpUrl httpUrl;

            Map<String, String> headers;

            @Override
            public OkHttpInvocationBuilder accept(com.github.dockerjava.core.MediaType mediaType) {
                return header("accept", mediaType.getMediaType());
            }

            @Override
            public OkHttpInvocationBuilder header(String name, String value) {
                Map<String, String> newHeaders = new HashMap<>(headers);
                newHeaders.put(name, value);
                return this.withHeaders(newHeaders);
            }

            @Override
            public void delete() {
                Request request = new Request.Builder().url(httpUrl).delete().build();
                try (Response response = execute(request)) {

                }
            }

            @Override
            public void get(ResultCallback<Frame> resultCallback) {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

                handleStreamedResponse(
                    execute(request),
                    resultCallback,
                    new FramedResponseStreamHandler(resultCallback)
                );
            }

            @Override
            @SneakyThrows(IOException.class)
            public <T> T get(TypeReference<T> typeReference) {
                try (InputStream inputStream = get()) {
                    return MAPPER.readValue(inputStream, typeReference);
                }
            }

            @Override
            public <T> void get(TypeReference<T> typeReference, ResultCallback<T> resultCallback) {
                // FIXME
                throw new IllegalStateException("doesn't seem to be used in docker-java");
            }

            @Override
            @SneakyThrows(IOException.class)
            public InputStream post(Object entity) {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .post(RequestBody.create(null, MAPPER.writeValueAsBytes(entity)))
                    .build();

                Response response = execute(request);
                return response.body().byteStream();
            }

            @Override
            @SneakyThrows(IOException.class)
            public <T> T post(Object entity, TypeReference<T> typeReference) {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .post(RequestBody.create(MediaType.parse("application/json"), MAPPER.writeValueAsBytes(entity)))
                    .build();

                try (Response response = execute(request)) {
                    String inputStream = response.body().string();
                    return MAPPER.readValue(inputStream, typeReference);
                }
            }

            @Override
            @SneakyThrows(JsonProcessingException.class)
            public <T> void post(Object entity, TypeReference<T> typeReference, ResultCallback<T> resultCallback) {
                post(typeReference, resultCallback, new ByteArrayInputStream(MAPPER.writeValueAsBytes(entity)));
            }

            @Override
            @SneakyThrows(IOException.class)
            public <T> T post(TypeReference<T> typeReference, InputStream body) {
                try (InputStream inputStream = post(body)) {
                    return MAPPER.readValue(inputStream, typeReference);
                }
            }

            @Override
            @SneakyThrows(IOException.class)
            public void post(Object entity, InputStream stdin, ResultCallback<Frame> resultCallback) {

                Request request = new Request.Builder()
                    .url(httpUrl)
                    .post(RequestBody.create(MediaType.parse("application/json"), MAPPER.writeValueAsBytes(entity)))
                    .build();

                OkHttpClient okHttpClient = this.okHttpClient;

                if (stdin != null) {
                    // FIXME there must be a better way of handling it
                    okHttpClient = okHttpClient.newBuilder()
                        .addNetworkInterceptor(new Interceptor() {
                            @Override
                            @SneakyThrows
                            public Response intercept(Chain chain) {
                                RealConnection connection = (RealConnection) chain.connection();

                                Field sinkField = RealConnection.class.getDeclaredField("sink");
                                sinkField.setAccessible(true);
                                BufferedSink sink = (BufferedSink) sinkField.get(connection);

                                Thread thread = new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, () -> {
                                    try {
                                        sink.writeAll(Okio.source(stdin));
                                        sink.flush();

                                        Thread.sleep(100);
                                        sink.close();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                thread.start();

                                return chain.proceed(chain.request());
                            }
                        })
                        .build();
                }

                handleStreamedResponse(
                    execute(request),
                    resultCallback,
                    new FramedResponseStreamHandler(resultCallback)
                );
            }

            @Override
            public <T> void post(TypeReference<T> typeReference, ResultCallback<T> resultCallback, InputStream body) {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .post(toRequestBody(body, null))
                    .build();

                handleStreamedResponse(
                    execute(request),
                    resultCallback,
                    new JsonResponseCallbackHandler<T>(typeReference, resultCallback)
                );
            }

            protected <T> void handleStreamedResponse(Response response, ResultCallback<T> callback, SimpleChannelInboundHandler<ByteBuf> handler) {
                try {
                    callback.onStart(response);
                    BufferedSource source = response.body().source();

                    byte[] buffer = new byte[4 * 1024];
                    while (!source.exhausted()) {
                        InputStream inputStream = source.inputStream();
                        int bytesReaded = inputStream.read(buffer);

                        handler.channelRead(null, Unpooled.wrappedBuffer(buffer, 0, bytesReaded));
                    }
                    callback.onComplete();
                } catch (Exception e) {
                    callback.onError(e);
                } finally {
                    response.close();
                }
            }

            @Override
            public void postStream(InputStream body) {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .post(toRequestBody(body, null))
                    .build();

                try (Response response = execute(request)) {

                }
            }

            @Override
            public InputStream get() {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

                Response response = execute(request);
                return response.body().byteStream();
            }

            @Override
            public void put(InputStream body, com.github.dockerjava.core.MediaType mediaType) {
                Request request = new Request.Builder()
                    .url(httpUrl)
                    .put(toRequestBody(body, mediaType.toString()))
                    .build();

                try (Response response = execute(request)) {

                }
            }

            protected RequestBody toRequestBody(InputStream body, @Nullable String mediaType) {
                return new RequestBody() {
                    @Nullable
                    @Override
                    public okhttp3.MediaType contentType() {
                        if (mediaType == null) {
                            return null;
                        }
                        return okhttp3.MediaType.parse(mediaType);
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        sink.writeAll(Okio.source(body));

                    }
                };
            }

            @SneakyThrows(IOException.class)
            protected Response execute(Request request) {
                Response response = okHttpClient.newCall(request).execute();
                int code = response.code();
                if (code < 200 || code >= 300) {
                    try {
                        String body = response.body().string();
                        switch (code) {
                            case 304:
                                throw new NotModifiedException(body);
                            case 400:
                                throw new BadRequestException(body);
                            case 401:
                                throw new UnauthorizedException(body);
                            case 404:
                                throw new NotFoundException(body);
                            case 406:
                                throw new NotAcceptableException(body);
                            case 409:
                                throw new ConflictException(body);
                            case 500:
                                throw new InternalServerErrorException(body);
                            default:
                                throw new DockerException(body, code);
                        }
                    } finally {
                        response.close();
                    }
                } else {
                    return response;
                }
            }
        }
    }
}
