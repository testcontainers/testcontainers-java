package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.Must;

/**
 * Top element
 */
@Data
public abstract class Top {
    @Must
    private String objectClass;
}
