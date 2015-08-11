package org.testcontainers.utility;

import java.security.SecureRandom;

/**
 * Created by rnorth on 08/08/2015.
 */
public class Base58 {

    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
            .toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String randomString(int length) {
        char[] result = new char[length];

        for (int i = 0; i < length; i++) {
            char pick = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
            result[i] = pick;
        }

        return new String(result);
    }
}
