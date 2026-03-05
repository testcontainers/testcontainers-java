package org.testcontainers.cyrus;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Testcontainers implementation for Cyrus.
 * <p>
 * Supported images:
 * <ul>
 *     <li>{@code ghcr.io/cyrusimap/cyrus-docker-test-server}</li>
 *     <li>{@code cyrusimap/cyrus-docker-test-server}</li>
 * </ul>
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>IMAP: 8143</li>
 *     <li>POP3: 8110</li>
 *     <li>HTTP (JMAP/CardDAV/CalDAV): 8080</li>
 *     <li>LMTP: 8024</li>
 *     <li>SIEVE: 4190</li>
 *     <li>Management API: 8001</li>
 * </ul>
 * Supported environment variables:
 * <ul>
 *     <li>{@code REFRESH}</li>
 *     <li>{@code CYRUS_VERSION}</li>
 *     <li>{@code DEFAULTDOMAIN}</li>
 *     <li>{@code SERVERNAME}</li>
 *     <li>{@code RELAYHOST}</li>
 *     <li>{@code RELAYAUTH}</li>
 * </ul>
 */
public class CyrusContainer extends GenericContainer<CyrusContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "ghcr.io/cyrusimap/cyrus-docker-test-server"
    );

    private static final DockerImageName COMPATIBLE_IMAGE_NAME = DockerImageName.parse("cyrusimap/cyrus-docker-test-server");

    private static final int IMAP_PORT = 8143;

    private static final int POP3_PORT = 8110;

    private static final int HTTP_PORT = 8080;

    private static final int LMTP_PORT = 8024;

    private static final int SIEVE_PORT = 4190;

    private static final int MANAGEMENT_PORT = 8001;

    private static final int MANAGEMENT_REQUEST_TIMEOUT_MILLIS = 10_000;

    private static final String SKIP_CREATE_USERS_ENV = "SKIP_CREATE_USERS";

    private static final String[] DEFAULT_USERS = { "user1", "user2", "user3", "user4", "user5" };

    private final Map<String, CyrusUser> seedUsers = new LinkedHashMap<String, CyrusUser>();

    public CyrusContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public CyrusContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, COMPATIBLE_IMAGE_NAME);

        withExposedPorts(IMAP_PORT, POP3_PORT, HTTP_PORT, LMTP_PORT, SIEVE_PORT, MANAGEMENT_PORT);
        waitingFor(Wait.forHttp("/").forPort(MANAGEMENT_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
    }

    public int getImapPort() {
        return getMappedPort(IMAP_PORT);
    }

    public String getImapUrl() {
        return "imap://" + getHost() + ":" + getImapPort();
    }

    public int getPop3Port() {
        return getMappedPort(POP3_PORT);
    }

    public String getPop3Url() {
        return "pop3://" + getHost() + ":" + getPop3Port();
    }

    public int getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    public String getHttpBaseUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    public String getJmapUrl() {
        return getHttpBaseUrl() + "/jmap/";
    }

    public int getLmtpPort() {
        return getMappedPort(LMTP_PORT);
    }

    public int getSievePort() {
        return getMappedPort(SIEVE_PORT);
    }

    public int getManagementPort() {
        return getMappedPort(MANAGEMENT_PORT);
    }

    public String getManagementUrl() {
        return "http://" + getHost() + ":" + getManagementPort();
    }

    public CyrusContainer withDefaultDomain(String defaultDomain) {
        withEnv("DEFAULTDOMAIN", defaultDomain);
        return self();
    }

    public CyrusContainer withRefresh(boolean refresh) {
        if (refresh) {
            withEnv("REFRESH", "1");
        } else {
            getEnvMap().remove("REFRESH");
        }
        return self();
    }

    public CyrusContainer withCyrusVersion(String cyrusVersion) {
        withEnv("CYRUS_VERSION", cyrusVersion);
        return self();
    }

    public CyrusContainer withServerName(String serverName) {
        withEnv("SERVERNAME", serverName);
        return self();
    }

    public CyrusContainer withRelayHost(String relayHost) {
        withEnv("RELAYHOST", relayHost);
        return self();
    }

    public CyrusContainer withRelayAuth(String relayAuth) {
        withEnv("RELAYAUTH", relayAuth);
        return self();
    }

    public CyrusContainer withSkipCreateUsers(boolean skipCreateUsers) {
        if (skipCreateUsers) {
            withEnv(SKIP_CREATE_USERS_ENV, "1");
        } else {
            getEnvMap().remove(SKIP_CREATE_USERS_ENV);
        }
        return self();
    }

    public CyrusContainer withSeedUser(CyrusUser user) {
        registerSeedUser(validateUser(user));
        return self();
    }

    public CyrusContainer withSeedUsers(CyrusUser... users) {
        if (users == null) {
            throw new IllegalArgumentException("users must not be null");
        }
        for (CyrusUser user : users) {
            withSeedUser(user);
        }
        return self();
    }

    public CyrusContainer withSeedUsers(Iterable<CyrusUser> users) {
        if (users == null) {
            throw new IllegalArgumentException("users must not be null");
        }
        for (CyrusUser user : users) {
            withSeedUser(user);
        }
        return self();
    }

    public CyrusContainer withSeedEmptyUser(String userId) {
        return withSeedUser(CyrusUser.builder(userId).build());
    }

    public String exportUser(String userId) throws IOException {
        return executeManagementRequest("GET", userId, null);
    }

    public Optional<String> exportUserIfExists(String userId) throws IOException {
        ManagementResponse response = executeManagementRequestRaw("GET", userId, null);
        if (response.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            return Optional.empty();
        }
        assertSuccess(response);
        return Optional.of(response.getBody());
    }

    public void importUser(String userId, String jsonPayload) throws IOException {
        if (jsonPayload == null) {
            throw new IllegalArgumentException("jsonPayload must not be null");
        }
        executeManagementRequest("PUT", userId, jsonPayload);
    }

    public void upsertUser(CyrusUser user) throws IOException {
        CyrusUser validatedUser = validateUser(user);
        importUser(validatedUser.getUserId(), validatedUser.toJson());
    }

    public boolean createUserIfMissing(CyrusUser user) throws IOException {
        CyrusUser validatedUser = validateUser(user);
        if (userExists(validatedUser.getUserId())) {
            return false;
        }
        upsertUser(validatedUser);
        return true;
    }

    public boolean userExists(String userId) throws IOException {
        ManagementResponse response = executeManagementRequestRaw("GET", userId, null);
        if (response.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
        }
        assertSuccess(response);
        return true;
    }

    public void deleteUser(String userId) throws IOException {
        ManagementResponse existingUser = executeManagementRequestRaw("GET", userId, null);
        if (existingUser.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw createRequestFailedException(
                "DELETE",
                existingUser.getUrl(),
                HttpURLConnection.HTTP_NOT_FOUND,
                existingUser.getBody()
            );
        }
        assertSuccess(existingUser);
        executeManagementRequest("DELETE", userId, null);
    }

    public boolean deleteUserIfExists(String userId) throws IOException {
        ManagementResponse existingUser = executeManagementRequestRaw("GET", userId, null);
        if (existingUser.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
        }
        assertSuccess(existingUser);
        executeManagementRequest("DELETE", userId, null);
        return true;
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if ("1".equals(getEnvMap().get(SKIP_CREATE_USERS_ENV))) {
            deleteDefaultUsers();
        }
        applySeedUsers();
    }

    private String executeManagementRequest(String method, String userId, String payload) throws IOException {
        ManagementResponse response = executeManagementRequestRaw(method, userId, payload);
        assertSuccess(response);
        return response.getBody();
    }

    private ManagementResponse executeManagementRequestRaw(
        String method,
        String userId,
        String payload
    ) throws IOException {
        String effectiveUserId = normalizeUserId(userId);
        String url = getManagementUrl() + "/" + encodePathSegment(effectiveUserId);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(MANAGEMENT_REQUEST_TIMEOUT_MILLIS);
        connection.setReadTimeout(MANAGEMENT_REQUEST_TIMEOUT_MILLIS);

        if (payload != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(payloadBytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payloadBytes);
            }
        }

        try {
            int statusCode = connection.getResponseCode();
            String responseBody = readBody(statusCode < HttpURLConnection.HTTP_BAD_REQUEST
                ? connection.getInputStream()
                : connection.getErrorStream());
            return new ManagementResponse(method, url, statusCode, responseBody);
        } finally {
            connection.disconnect();
        }
    }

    private void assertSuccess(ManagementResponse response) throws IOException {
        if (response.getStatusCode() >= HttpURLConnection.HTTP_OK &&
            response.getStatusCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
            return;
        }
        throw createRequestFailedException(response);
    }

    private IOException createRequestFailedException(ManagementResponse response) {
        return createRequestFailedException(
            response.getMethod(),
            response.getUrl(),
            response.getStatusCode(),
            response.getBody()
        );
    }

    private IOException createRequestFailedException(String method, String url, int statusCode, String responseBody) {
        String body = responseBody == null || responseBody.isEmpty() ? "<empty>" : responseBody;
        return new IOException(
            String.format(
                "Cyrus management request failed: %s %s returned HTTP %d with body: %s",
                method,
                url,
                statusCode,
                body
            )
        );
    }

    private void registerSeedUser(CyrusUser user) {
        seedUsers.remove(user.getUserId());
        seedUsers.put(user.getUserId(), user);
    }

    private void applySeedUsers() {
        for (CyrusUser user : seedUsers.values()) {
            try {
                upsertUser(user);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to seed Cyrus user '" + user.getUserId() + "'", e);
            }
        }
    }

    private void deleteDefaultUsers() {
        for (String user : DEFAULT_USERS) {
            try {
                deleteUserIfExists(user);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to delete default Cyrus user '" + user + "'", e);
            }
        }
    }

    private static CyrusUser validateUser(CyrusUser user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return user;
    }

    private static String normalizeUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        return userId.trim();
    }

    private static String encodePathSegment(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    private static String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        try (InputStream in = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static final class ManagementResponse {

        private final String method;

        private final String url;

        private final int statusCode;

        private final String body;

        private ManagementResponse(String method, String url, int statusCode, String body) {
            this.method = method;
            this.url = url;
            this.statusCode = statusCode;
            this.body = body;
        }

        private String getMethod() {
            return method;
        }

        private String getUrl() {
            return url;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getBody() {
            return body;
        }
    }
}
