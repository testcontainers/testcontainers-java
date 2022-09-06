package org.testcontainers.containers;

import com.google.cloud.tools.jib.api.JibContainer;
import org.testcontainers.utility.LazyFuture;

public class JibImage extends LazyFuture<String> {

    private String image;

    public JibImage(JibContainer jibContainer) {
        this.image = jibContainer.getTargetImage().toString();
    }

    @Override
    protected String resolve() {
        return this.image;
    }
}
