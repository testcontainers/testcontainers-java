package org.testcontainers.containers.image;

import java.util.concurrent.Future;
import org.testcontainers.containers.image.pull.policy.ImagePullPolicy;
import org.testcontainers.containers.image.pull.policy.PullPolicy;
import org.testcontainers.images.RemoteDockerImage;

public class ImageNameResolverProvider {

    private Future<String> delegate;

    private ImagePullPolicy imagePullPolicy;

    private String imageName;

    public ImageNameResolverProvider(String imageName) {
        this(imageName, PullPolicy.getDefaultPullPolicy());
    }

    public ImageNameResolverProvider(String imageName, ImagePullPolicy policy) {
        this.imageName = imageName;
        this.imagePullPolicy = policy;
    }

    public ImageNameResolverProvider(Future<String> delegate) {
        this.delegate = delegate;
    }

    public void setImagePullPolicy(ImagePullPolicy policy) {
        this.imagePullPolicy = policy;
    }

    public Future<String> getResolver() {
        if (delegate != null) {
            return delegate;
        }
        return new RemoteDockerImage(imageName, imagePullPolicy);
    }
}
