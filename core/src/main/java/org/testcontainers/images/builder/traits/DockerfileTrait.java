package org.testcontainers.images.builder.traits;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * BuildContextBuilder's trait for Dockerfile-based resources.
 *
 */
public interface DockerfileTrait<SELF extends DockerfileTrait<SELF> & BuildContextBuilderTrait<SELF> & StringsTrait<SELF>> {

    default SELF withDockerfileFromBuilder(Consumer<DockerfileBuilder> builderConsumer) {

        DockerfileBuilder builder = new DockerfileBuilder();

        builderConsumer.accept(builder);

        // return Transferable because we want to build Dockerfile's content lazily
        return ((SELF) this).withFileFromTransferable("Dockerfile", new Transferable() {

            @Getter(lazy = true)
            private final byte[] bytes = builder.build().getBytes();

            @Override
            public long getSize() {
                return getBytes().length;
            }

            @Override
            public void transferTo(OutputStream outputStream) {
                try {
                    IOUtils.write(getBytes(), outputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Can't transfer Dockerfile", e);
                }
            }
        });
    }
}
