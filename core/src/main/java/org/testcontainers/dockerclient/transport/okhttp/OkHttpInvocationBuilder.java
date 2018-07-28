package org.testcontainers.dockerclient.transport.okhttp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.BadRequestException;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotAcceptableException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.exception.UnauthorizedException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.netty.handler.FramedResponseStreamHandler;
import com.github.dockerjava.netty.handler.JsonResponseCallbackHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.connection.RealConnection;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class OkHttpInvocationBuilder implements InvocationBuilder {

    ObjectMapper objectMapper;

    OkHttpClient okHttpClient;

    Request.Builder requestBuilder;

    public OkHttpInvocationBuilder(ObjectMapper objectMapper, OkHttpClient okHttpClient, HttpUrl httpUrl) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;

        requestBuilder = new Request.Builder()
            .url(httpUrl);
    }

    @Override
    public OkHttpInvocationBuilder accept(com.github.dockerjava.core.MediaType mediaType) {
        return header("accept", mediaType.getMediaType());
    }

    @Override
    public OkHttpInvocationBuilder header(String name, String value) {
        requestBuilder.header(name, value);
        return this;
    }

    @Override
    @SneakyThrows
    public void delete() {
        Request request = requestBuilder
            .delete()
            .build();

        execute(request).close();
    }

    @Override
    @SneakyThrows
    public void get(ResultCallback<Frame> resultCallback) {
        Request request = requestBuilder
            .get()
            .build();

        executeAndStream(
            request,
            resultCallback,
            new FramedResponseStreamHandler(resultCallback)
        );
    }

    @Override
    @SneakyThrows(IOException.class)
    public <T> T get(TypeReference<T> typeReference) {
        try (InputStream inputStream = get()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    @Override
    public <T> void get(TypeReference<T> typeReference, ResultCallback<T> resultCallback) {
        // FIXME
        throw new IllegalStateException("doesn't seem to be used in docker-java");
    }

    @Override
    @SneakyThrows
    public InputStream post(Object entity) {
        Request request = requestBuilder
            .post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsBytes(entity)))
            .build();

        return execute(request).body().byteStream();
    }

    @Override
    @SneakyThrows
    public <T> T post(Object entity, TypeReference<T> typeReference) {
        Request request = requestBuilder
            .post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsBytes(entity)))
            .build();

        try (Response response = execute(request)) {
            String inputStream = response.body().string();
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public <T> void post(Object entity, TypeReference<T> typeReference, ResultCallback<T> resultCallback) {
        post(typeReference, resultCallback, new ByteArrayInputStream(objectMapper.writeValueAsBytes(entity)));
    }

    @Override
    @SneakyThrows(IOException.class)
    public <T> T post(TypeReference<T> typeReference, InputStream body) {
        try (InputStream inputStream = post(body)) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    @Override
    @SneakyThrows
    public void post(Object entity, InputStream stdin, ResultCallback<Frame> resultCallback) {
        Request request = requestBuilder
            .post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsBytes(entity)))
            .build();

        OkHttpClient okHttpClient = this.okHttpClient;

        if (stdin != null) {
            // FIXME there must be a better way of handling it
            okHttpClient = okHttpClient.newBuilder()
                .addNetworkInterceptor(chain -> {
                    Response response = chain.proceed(chain.request());
                    if (response.isSuccessful()) {
                        Thread thread = new Thread() {
                            @Override
                            @SneakyThrows
                            public void run() {
                                Field sinkField = RealConnection.class.getDeclaredField("sink");
                                sinkField.setAccessible(true);

                                try (
                                    BufferedSink sink = (BufferedSink) sinkField.get(chain.connection());
                                    Source source = Okio.source(stdin);
                                ) {
                                    sink.writeAll(source);
                                }
                            }
                        };
                        thread.start();
                    }
                    return response;
                })
                .build();
        }

        executeAndStream(
            okHttpClient,
            request,
            resultCallback,
            new FramedResponseStreamHandler(resultCallback)
        );
    }

    @Override
    public <T> void post(TypeReference<T> typeReference, ResultCallback<T> resultCallback, InputStream body) {
        Request request = requestBuilder
            .post(toRequestBody(body, null))
            .build();

        executeAndStream(
            request,
            resultCallback,
            new JsonResponseCallbackHandler<>(typeReference, resultCallback)
        );
    }

    @Override
    @SneakyThrows
    public void postStream(InputStream body) {
        Request request = requestBuilder
            .post(toRequestBody(body, null))
            .build();

        execute(request).close();
    }

    @Override
    @SneakyThrows
    public InputStream get() {
        Request request = requestBuilder
            .get()
            .build();

        return execute(request).body().byteStream();
    }

    @Override
    @SneakyThrows
    public void put(InputStream body, com.github.dockerjava.core.MediaType mediaType) {
        Request request = requestBuilder
            .put(toRequestBody(body, mediaType.toString()))
            .build();

        execute(request).close();
    }

    protected RequestBody toRequestBody(InputStream body, @Nullable String mediaType) {
        return new RequestBody() {
            @Nullable
            @Override
            public MediaType contentType() {
                if (mediaType == null) {
                    return null;
                }
                return MediaType.parse(mediaType);
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try(Source source = Okio.source(body)) {
                    sink.writeAll(source);
                }
            }
        };
    }

    protected Response execute(Request request) {
        return execute(okHttpClient, request);
    }

    @SneakyThrows(IOException.class)
    protected Response execute(OkHttpClient okHttpClient, Request request) {
        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            String body = response.body().string();
            switch (response.code()) {
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
                    throw new DockerException(body, response.code());
            }
        } else {
            return response;
        }
    }

    protected <T> void executeAndStream(Request request, ResultCallback<T> callback, SimpleChannelInboundHandler<ByteBuf> handler) {
        executeAndStream(okHttpClient, request, callback, handler);
    }

    protected <T> void executeAndStream(OkHttpClient okHttpClient, Request request, ResultCallback<T> callback, SimpleChannelInboundHandler<ByteBuf> handler) {
        // TODO proper thread management
        Thread thread = new Thread() {
            @Override
            @SneakyThrows
            public void run() {
                try (
                    Response response = execute(okHttpClient, request.newBuilder().tag("streaming").build());
                    BufferedSource source = response.body().source();
                    InputStream inputStream = source.inputStream();
                ) {
                    AtomicBoolean shouldStop = new AtomicBoolean();
                    callback.onStart(() -> {
                        shouldStop.set(true);
                        response.close();
                    });

                    byte[] buffer = new byte[4 * 1024];
                    while (!(shouldStop.get() || source.exhausted())) {
                        int bytesReceived = inputStream.read(buffer);

                        handler.channelRead(null, Unpooled.wrappedBuffer(buffer, 0, bytesReceived));
                    }
                    callback.onComplete();
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        };

        thread.start();
    }
}
