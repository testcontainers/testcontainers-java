package org.testcontainers.junit.support;

import lombok.Data;

/**
 * Represents the model object in the OpenAPI example.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@Data
public class Pet {
    private Integer id;
    private String name;
}
