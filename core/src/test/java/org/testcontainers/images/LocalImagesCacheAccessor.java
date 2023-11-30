package org.testcontainers.images;

public final class LocalImagesCacheAccessor {

    public static synchronized void clearCache() {
        LocalImagesCache.INSTANCE.cache.clear();
        LocalImagesCache.INSTANCE.initialized.set(false);
    }
}
