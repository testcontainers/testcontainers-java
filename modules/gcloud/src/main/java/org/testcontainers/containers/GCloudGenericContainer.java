package org.testcontainers.containers;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eddú Meléndez
 */
public class GCloudGenericContainer<SELF extends GCloudGenericContainer<SELF>> extends GenericContainer<SELF> {

    public static final String DEFAULT_GCLOUD_IMAGE = "gcr.io/google.com/cloudsdktool/cloud-sdk:306.0.0";

    private List<String> commands = new ArrayList<>();

    public GCloudGenericContainer(String image) {
        super(image);
    }

}
