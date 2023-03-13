package org.testcontainers.jib;

import com.google.cloud.tools.jib.api.DescriptorDigest;
import com.google.cloud.tools.jib.api.ImageDetails;

import java.security.DigestException;
import java.util.ArrayList;
import java.util.List;

class JibImageDetails implements ImageDetails {

    private long size;

    private String imageId;

    private List<String> layers;

    public JibImageDetails(long size, String imageId, List<String> layers) {
        this.size = size;
        this.imageId = imageId;
        this.layers = layers;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public DescriptorDigest getImageId() throws DigestException {
        return DescriptorDigest.fromDigest(this.imageId);
    }

    @Override
    public List<DescriptorDigest> getDiffIds() throws DigestException {
        List<DescriptorDigest> processedDiffIds = new ArrayList<>(this.layers.size());
        for (String diffId : this.layers) {
            processedDiffIds.add(DescriptorDigest.fromDigest(diffId.trim()));
        }
        return processedDiffIds;
    }
}
