package org.testcontainers.dockerclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.util.FiltersEncoder;
import com.github.dockerjava.netty.MediaType;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.github.dockerjava.netty.WebTarget;
import com.github.dockerjava.netty.exec.AbstrSyncDockerCmdExec;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

class TestcontainersDockerCmdExecFactory extends NettyDockerCmdExecFactory {

    @Override
    @SneakyThrows
    public ListNetworksCmd.Exec createListNetworksCmdExec() {
        Field baseResourceField = NettyDockerCmdExecFactory.class.getDeclaredField("baseResource");
        baseResourceField.setAccessible(true);

        // FIXME Workaround for https://github.com/docker-java/docker-java/issues/857
        return new ListNetworksCmdExec((WebTarget) baseResourceField.get(this), getDockerClientConfig());
    }

    private static class ListNetworksCmdExec extends AbstrSyncDockerCmdExec<ListNetworksCmd, List<Network>> implements ListNetworksCmd.Exec {

        private static final Logger LOGGER = LoggerFactory.getLogger(com.github.dockerjava.netty.exec.ListNetworksCmdExec.class);

        public ListNetworksCmdExec(WebTarget baseResource, DockerClientConfig dockerClientConfig) {
            super(baseResource, dockerClientConfig);
        }

        @Override
        protected List<Network> execute(ListNetworksCmd command) {
            WebTarget webTarget = getBaseResource().path("/networks");

            if (command.getFilters() != null && !command.getFilters().isEmpty()) {
                // Next line was changed (urlPathSegmentEscaper was removed)
                webTarget = webTarget.queryParam("filters", FiltersEncoder.jsonEncode(command.getFilters()));
            }

            LOGGER.trace("GET: {}", webTarget);

            return webTarget.request().accept(MediaType.APPLICATION_JSON).get(new TypeReference<List<Network>>() {
            });
        }
    }

}