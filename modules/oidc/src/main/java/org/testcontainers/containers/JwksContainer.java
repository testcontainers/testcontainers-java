package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * TODO add docs
 */
public class JwksContainer extends GenericContainer<JwksContainer> {

    private static final int DEFAULT_PORT = 80;
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("jtyr/asmttpd");
    private static final String DEFAULT_TAG = "0.4.5-1";

    public JwtContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG););
    }

    public JwtContainer(@NotNull DockerImageName image) {
        super(image);
        image.assertCompatibleWith(new DockerImageName[]{DEFAULT_IMAGE_NAME});
    }

    protected void configure() {
        super.configure();
        this.copyResources();
        this.setWaitStrategy((WaitStrategy) (new HttpWaitStrategy()).forPort(80).forPath("/jwks.json"));
        this.addExposedPort(80);
    }

    public void start() {
        super.start();
        this.createOpenidConfiguration();
    }

    @NotNull
    public final URL host() {
        try {
            return new URL(this.baseUrl("http", 80).toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public final String issuer() {
        return "" + this.host() + '/';
    }

    @NotNull
    public final URL baseUrl() throws MalformedURLException {
        return baseUrl("http", 80);
    }

    @NotNull
    public final URL baseUrl(@NotNull String scheme, int port) throws MalformedURLException {
        return baseUrl(scheme, 80);
    }

    @NotNull
    public final URL baseUrl(int port) throws MalformedURLException {
        return baseUrl("http", port);
    }

    @NotNull
    public final URL baseUrl(@NotNull String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + this.getHost() + ":" + this.getMappedPort(port));
    }

    @NotNull
    public final TokenForgery forgery() {
        return new TokenForgery(this.issuer().toString());
    }

    private final void copyResources() {
        MountableFile file = MountableFile.forClasspathResource("/security/jwt/public/jwks.json");
        this.withCopyFileToContainer(file, "/www/jwks.json");
    }

    private final void createOpenidConfiguration() {
        Map openidConfig = Map.of(
            "issuer", "" + this.host() + '/',
            "jwks_uri", this.host() + "/jwks.json"
        );
        String content = (new ObjectMapper()).writeValueAsString(openidConfig);
    }

    private final void execAction(String... command) {
        ExecResult result = this.execInContainer((String[]) Arrays.copyOf(command, command.length));
        Intrinsics.checkNotNullExpressionValue(result, "result");
        if (result.getExitCode() != 0) {
            throw (Throwable) (new JwtContainerConfigurationException(command[0], result));
        }
    }
}

