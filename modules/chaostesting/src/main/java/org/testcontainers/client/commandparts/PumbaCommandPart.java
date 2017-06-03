package org.testcontainers.client.commandparts;

/**
 * Created by novy on 01.01.17.
 */
public interface PumbaCommandPart {

    String evaluate();

    default PumbaCommandPart append(PumbaCommandPart other) {
        return () -> (evaluate() + " " + other.evaluate()).trim();
    }
}
