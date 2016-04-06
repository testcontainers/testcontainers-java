package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.KeyValuesStatement;

import java.util.Collections;
import java.util.Map;

public interface LabelStatementTrait<SELF extends LabelStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF label(String key, String value) {
        return label(Collections.singletonMap(key, value));
    }

    default SELF label(Map<String, String> entries) {
        return ((SELF) this).withStatement(new KeyValuesStatement("LABEL", entries));
    }
}
