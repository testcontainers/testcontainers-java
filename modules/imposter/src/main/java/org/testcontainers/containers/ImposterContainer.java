package org.testcontainers.containers;

import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

/**
 * Mocks from OpenAPI/Swagger specifications using Imposter.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterContainer<SELF extends ImposterContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {
    public static final int IMPOSTER_DEFAULT_PORT = 8080;
    public static final String COMBINED_SPECIFICATION_URL = "/_spec/combined.json";
    public static final String SPECIFICATION_UI_URL = "/_spec/";

    public ImposterContainer() {
        this("outofcoffee/imposter-openapi:1.2.0");
    }

    public ImposterContainer(String dockerImageName) {
        super(dockerImageName);

        addExposedPort(IMPOSTER_DEFAULT_PORT);
        setCommand(
            "--plugin", "openapi",
            "--configDir", "/opt/imposter/config"
        );

        // wait for the engine to parse and combine the specifications
        waitingFor(Wait.forHttp(ImposterContainer.COMBINED_SPECIFICATION_URL));
    }

    @Override
    protected void doStart() {
        super.doStart();
        logger().debug("Started Imposter mock engine\n  Specification UI: " + getSpecificationUiUri());
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(IMPOSTER_DEFAULT_PORT));
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }

    /**
     * The directory containing the OpenAPI/Swagger specification.
     *
     * @param specificationDir the directory
     */
    public void setSpecificationDir(String specificationDir) {
        addFileSystemBind(specificationDir, "/opt/imposter/config", BindMode.READ_ONLY);
    }

    public SELF withSpecificationDir(String htmlContentPath) {
        this.setSpecificationDir(htmlContentPath);
        return self();
    }

    public URI getCombinedSpecificationUri() {
        try {
            return URI.create(getBaseUrl("http", IMPOSTER_DEFAULT_PORT) + COMBINED_SPECIFICATION_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error getting combined specification URI", e);
        }
    }

    public URI getSpecificationUiUri() {
        try {
            return URI.create(getBaseUrl("http", IMPOSTER_DEFAULT_PORT) + SPECIFICATION_UI_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error getting specification UI URI", e);
        }
    }
}
