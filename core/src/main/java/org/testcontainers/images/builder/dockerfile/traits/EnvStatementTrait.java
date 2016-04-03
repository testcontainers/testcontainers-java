package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.KeyValuesStatement;

import java.util.Collections;
import java.util.Map;

public interface EnvStatementTrait<SELF extends EnvStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF env(String key, String value) {
        return env(Collections.singletonMap(key, value));
    }

    default SELF env(Map<String, String> entries) {
        return ((SELF) this).withStatement(new KeyValuesStatement("ENV", entries));
    }
}
