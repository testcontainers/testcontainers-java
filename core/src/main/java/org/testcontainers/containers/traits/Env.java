package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.google.common.collect.ObjectArrays;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class Env<SELF extends Container<SELF>> implements Trait<SELF>  {

    public interface Support<SELF extends Container<SELF>> extends SelfReference<SELF> {

        /**
         * Add an environment variable to be passed to the container. Consider using {@link #withEnv(String, String)}
         * for building a container in a fluent style.
         *
         * @param key   environment variable key
         * @param value environment variable value
         */
        default void addEnv(String key, String value) {
            self().with(new Env<>(key + "=" + value));
        }

        /**
         * Add an environment variable to be passed to the container.
         *
         * @param key   environment variable key
         * @param value environment variable value
         * @return this
         */
        default SELF withEnv(String key, String value) {
            addEnv(key, value);

            return self();
        }

        default List<String> getEnv() {
            Stream<Env> traits = self().getTraits(Env.class);
            return traits.map(Env::getValue).collect(Collectors.toList());
        }

        default void setEnv(List<String> env) {
            self().replaceTraits(Env.class, env.stream().map(Env::new));
        }
    }

    protected final String value;

    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        String[] env = createContainerCmd.getEnv();

        if (env == null) {
            env = new String[] {};
        }

        createContainerCmd.withEnv(ObjectArrays.concat(env, value));
    }
}
