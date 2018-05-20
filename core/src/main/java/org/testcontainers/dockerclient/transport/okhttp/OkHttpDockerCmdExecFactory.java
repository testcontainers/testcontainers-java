package org.testcontainers.dockerclient.transport.okhttp;

import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.AbstractDockerCmdExecFactory;
import com.github.dockerjava.core.WebTarget;
import com.github.dockerjava.core.exec.PingCmdExec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class OkHttpDockerCmdExecFactory extends AbstractDockerCmdExecFactory {

    @Override
    protected WebTarget getBaseResource() {
        return new OkHttpWebTarget(
            getDockerClientConfig(),
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

}
