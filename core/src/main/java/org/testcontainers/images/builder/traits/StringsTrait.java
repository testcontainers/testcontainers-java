package org.testcontainers.images.builder.traits;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.images.builder.Transferable;

/**
 * BuildContextBuilder's trait for String-based manipulations.
 *
 */
public interface StringsTrait<SELF extends StringsTrait<SELF> & BuildContextBuilderTrait<SELF>> {

    default SELF withFileFromString(String path, String content) {
        return ((SELF) this).withFileFromTransferable(path, new Transferable() {

            @Getter
            byte[] bytes = content.getBytes();

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public String getDescription() {
                return "String: " + StringUtils.abbreviate(content, 100);
            }
        });
    }
}
