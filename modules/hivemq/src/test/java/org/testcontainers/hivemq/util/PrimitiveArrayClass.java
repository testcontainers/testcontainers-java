package org.testcontainers.hivemq.util;

/**
 * Helper class that generates a primitive array descriptor ("[B")
 * in the constant pool through a byte array cast.
 */
public class PrimitiveArrayClass {

    public void castToByteArray(Object value) {
        byte[] ignored = (byte[]) value;
    }
}
