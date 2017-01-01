package org.testcontainers;

/**
 * Created by novy on 01.01.17.
 */
interface PumbaCommandPart {

    String evaluate();

    default PumbaCommandPart append(PumbaCommandPart other) {
        return () -> (evaluate() + " " + other.evaluate()).trim();
    }
}
