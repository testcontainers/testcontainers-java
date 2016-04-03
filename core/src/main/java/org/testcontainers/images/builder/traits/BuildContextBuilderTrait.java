package org.testcontainers.images.builder.traits;

import org.testcontainers.images.builder.Transferable;

/**
 * base BuildContextBuilder's trait
 *
 */
public interface BuildContextBuilderTrait<SELF extends BuildContextBuilderTrait<SELF>> {

    SELF withFileFromTransferable(String path, Transferable transferable);
}
