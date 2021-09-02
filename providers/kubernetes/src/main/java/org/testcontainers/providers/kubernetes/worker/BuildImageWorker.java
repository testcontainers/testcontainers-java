package org.testcontainers.providers.kubernetes.worker;

import com.github.dockerjava.api.async.ResultCallback;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.event.Level;
import org.testcontainers.controller.intents.BuildResultItem;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.NullInputStream;
import org.testcontainers.providers.kubernetes.io.LoggingOutputStream;
import org.testcontainers.providers.kubernetes.model.KanikoBuildParams;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

@Slf4j // TODO: Complete or remove
public class BuildImageWorker implements Closeable {

    private final Thread workerThread;
    private final KubernetesContext ctx;
    private final InputStream in;
    private final Pod createdPod;
    private final KanikoBuildParams buildParams;
    private final ResultCallback<BuildResultItem> callback;


    public BuildImageWorker(
        KubernetesContext ctx,
        InputStream in,
        Pod createdPod,
        KanikoBuildParams buildParams,
        ResultCallback<BuildResultItem> callback
    ) {
        this.ctx = ctx;
        this.in = in;
        this.createdPod = createdPod;
        this.buildParams = buildParams;
        this.callback = callback;
        this.workerThread = new Thread(this::buildImage);
    }


    @SneakyThrows
    private void buildImage() {
        try {
            callback.onStart(this);
            log.info("Transferring build context");
            File tempFile = File.createTempFile("testcontainers-", null).getCanonicalFile();
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                IOUtils.copy(in, out);
            }

            ctx.getClient().pods()
                .inNamespace(createdPod.getMetadata().getNamespace())
                .withName(createdPod.getMetadata().getName())
                .file("/workspace/context.tar.gz")
                .upload(tempFile.toPath());

            String[] cmd = buildParams.createBuildCommand();

            CountDownLatch countDownLatch = new CountDownLatch(1);

            LoggingOutputStream outAdapter = new LoggingOutputStream(log, Level.INFO);
            LoggingOutputStream errAdapter = new LoggingOutputStream(log, Level.ERROR);


            ExecWatch exec = ctx.getClient().pods()
                .inNamespace(createdPod.getMetadata().getNamespace())
                .withName(createdPod.getMetadata().getName())
                .readingInput(new NullInputStream())
                .writingOutput(outAdapter) // TODO: What stream?
                .writingError(errAdapter)
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {

                    }


                    @Override
                    public void onFailure(Throwable t, Response response) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        callback.onNext(KubernetesBuildResultItem.success(buildParams.getTag()));
                        countDownLatch.countDown();
                    }
                })
                .exec(cmd);

            countDownLatch.await();

        }finally {
            ctx.getClient().pods()
                .delete(createdPod);
            callback.onComplete();
        }
    }

    @Override
    public void close() throws IOException {
        workerThread.interrupt();
    }

    public void start() {
        this.workerThread.start();
    }
}
