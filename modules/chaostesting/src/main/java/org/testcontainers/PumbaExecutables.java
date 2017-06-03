package org.testcontainers;

import org.testcontainers.client.PumbaExecutable;

/**
 * Created by novy on 03.06.17.
 */
public class PumbaExecutables {

    public static PumbaExecutable dockerized() {
        return new PumbaContainer();
    }
}
