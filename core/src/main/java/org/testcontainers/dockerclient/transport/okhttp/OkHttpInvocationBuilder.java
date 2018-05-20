package org.testcontainers.dockerclient.transport.okhttp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.netty.handler.FramedResponseStreamHandler;
import com.github.dockerjava.netty.handler.JsonResponseCallbackHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.connection.RealConnection;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.testcontainers.DockerClientFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@FieldDefaults(makeFinal = true)
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

        executeAndGet(request).close();
    }

    @Override
    @SneakyThrows
    public void get(ResultCallback<Frame> resultCallback) {
        Request request = requestBuilder
            .get()
            .build();

        execute(request).whenCompleteAsync((response, e) -> {
            if (e != null) {
                resultCallback.onError(e);
            } else {
                handleStreamedResponse(
                    response,
                    resultCallback,
                    new FramedResponseStreamHandler(resultCallback)
                );
            }
        });
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
            .post(RequestBody.create(null, objectMapper.writeValueAsBytes(entity)))
            .build();

        return executeAndGet(request).body().byteStream();
    }

    @Override
    @SneakyThrows
    public <T> T post(Object entity, TypeReference<T> typeReference) {
        Request request = requestBuilder
            .post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsBytes(entity)))
            .build();

        try (Response response = executeAndGet(request)) {
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

        execute(okHttpClient, request).whenCompleteAsync((response, e) -> {
            if (e != null) {
                resultCallback.onError(e);
            } else {
                handleStreamedResponse(
                    response,
                    resultCallback,
                    new FramedResponseStreamHandler(resultCallback)
                );
            }
        });
    }

    @Override
    public <T> void post(TypeReference<T> typeReference, ResultCallback<T> resultCallback, InputStream body) {
        Request request = requestBuilder
            .post(toRequestBody(body, null))
            .build();

        execute(request).whenCompleteAsync((response, e) -> {
            if (e != null) {
                resultCallback.onError(e);
            } else {

                handleStreamedResponse(
                    response,
                    resultCallback,
                    new JsonResponseCallbackHandler<>(typeReference, resultCallback)
                );
            }
        });
    }

    @Override
    @SneakyThrows
    public void postStream(InputStream body) {
        Request request = requestBuilder
            .post(toRequestBody(body, null))
            .build();

        executeAndGet(request).close();
    }

    @Override
    @SneakyThrows
    public InputStream get() {
        Request request = requestBuilder
            .get()
            .build();

        return executeAndGet(request).body().byteStream();
    }

    @Override
    @SneakyThrows
    public void put(InputStream body, com.github.dockerjava.core.MediaType mediaType) {
        Request request = requestBuilder
            .put(toRequestBody(body, mediaType.toString()))
            .build();

        executeAndGet(request).close();
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
                sink.writeAll(Okio.source(body));
            }
        };
    }

    @SneakyThrows
    protected Response executeAndGet(Request request) {
        try {
            return execute(request).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    protected CompletableFuture<Response> execute(Request request) {
        return execute(okHttpClient, request);
    }

    protected CompletableFuture<Response> execute(OkHttpClient okHttpClient, Request request) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    } finally {
                        response.close();
                    }
                } else {
                    future.complete(response);
                }
            }
        });
        return future;
    }

    protected <T> void handleStreamedResponse(Response response, ResultCallback<T> callback, SimpleChannelInboundHandler<ByteBuf> handler) {
        try {
            // TODO proper thread management
            Thread thread = new Thread(new Runnable() {
                @Override
                @SneakyThrows
                public void run() {
                    BufferedSource source = response.body().source();
                    InputStream inputStream = source.inputStream();

                    byte[] buffer = new byte[4 * 1024];
                    while (!source.exhausted() && !Thread.interrupted()) {
                        int bytesReceived = inputStream.read(buffer);

                        handler.channelRead(null, Unpooled.wrappedBuffer(buffer, 0, bytesReceived));
                    }
                    callback.onComplete();
                }
            });

            callback.onStart(() -> {
                thread.interrupt();
                response.close();
            });

            thread.start();
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
