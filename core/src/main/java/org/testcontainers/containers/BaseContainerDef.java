package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.UnstableAPI;
import org.testcontainers.images.RemoteDockerImage;

import java.util.HashMap;
import java.util.Map;

@Getter(AccessLevel.MODULE)
@RequiredArgsConstructor
@UnstableAPI
@Slf4j
abstract class BaseContainerDef {

    @NonNull
    @Setter(AccessLevel.MODULE)
    RemoteDockerImage image;

    Map<String, String> env = new HashMap<>();

    protected void putEnv(@NonNull String key, String value) {
        env.put(key, value);
    }

    protected void setEnv(Map<String, String> env) {
        this.env = new HashMap<>(env);
    }

    protected void applyTo(CreateContainerCmd createCommand) {
        createCommand.withEnv(
            env.entrySet().stream()
                .filter(it -> it.getValue() != null)
                .map(it -> it.getKey() + "=" + it.getValue())
                .toArray(String[]::new)
        );
    }
}
