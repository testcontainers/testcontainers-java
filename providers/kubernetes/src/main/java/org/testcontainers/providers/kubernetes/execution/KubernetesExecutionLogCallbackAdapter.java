package org.testcontainers.providers.kubernetes.execution;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class KubernetesExecutionLogCallbackAdapter<T extends ResultCallback<Frame>> extends OutputStream {
    private final StreamType streamType;
    private final T resultCallback;

    public KubernetesExecutionLogCallbackAdapter(StreamType streamType, T resultCallback) {
        this.streamType = streamType;
        this.resultCallback = resultCallback;
    }

    @Override
    public void write(int b) throws IOException {
        resultCallback.onNext(new Frame(streamType, new byte[]{ (byte) b }));
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        resultCallback.onNext(new Frame(streamType, Arrays.copyOfRange(b, off, len)));
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        resultCallback.onNext(new Frame(streamType, b));
    }

    @Override
    public void close() throws IOException {
        resultCallback.onComplete();
        resultCallback.close();
        super.close();
    }
}
