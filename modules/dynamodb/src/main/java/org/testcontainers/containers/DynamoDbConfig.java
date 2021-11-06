package org.testcontainers.containers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.util.Asserts;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;

import java.io.File;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DynamoDbConfig {

    public static final int DEFAULT_PORT = 8000;
    public static final String DEFAULT_COMMAND = String.format("-jar DynamoDBLocal.jar -port %s", DEFAULT_PORT);

    @Getter
    private Integer port;
    private boolean inMemory;
    private boolean delayTransientStatuses;
    private String dbPath;
    private boolean sharedDb;
    private String cors;
    private boolean optimizeDbBeforeStartup;

    @Override
    public String toString() {
        StringBuilder config = new StringBuilder("-jar DynamoDBLocal.jar -port ");
        if (port != null) {
            config.append(port);
        } else {
            config.append(DEFAULT_PORT);
        }

        if (inMemory) {
            config.append(" -inMemory");
        }

        if (delayTransientStatuses) {
            config.append(" -delayTransientStatuses");
        }

        if (StringUtils.isNotEmpty(dbPath)) {
            config.append(" -dbPath ")
                  .append(dbPath);
        }

        if (sharedDb) {
            config.append(" -sharedDb");
        }

        if (StringUtils.isNotEmpty(cors)) {
            config.append(" -cors ")
                  .append(cors);
        }

        if (optimizeDbBeforeStartup) {
            config.append(" -optimizeDbBeforeStartup");
        }

        return config.toString();
    }

    public static class DynamoDbConfigBuilder {

        private Integer port;
        private boolean inMemory;
        private boolean delayTransientStatuses;
        private String dbPath;
        private boolean sharedDb;
        private String cors;
        private boolean optimizeDbBeforeStartup;

        public DynamoDbConfigBuilder port(@NonNull Integer port) {
            Asserts.check(port > 0, "Port cannot be a negative value.");
            this.port = port;
            return this;
        }

        public DynamoDbConfigBuilder inMemory(boolean inMemory) {
            Asserts.check(inMemory && StringUtils.isEmpty(dbPath),
                         "You can't specify both -dbPath and -inMemory at once.");
            this.inMemory = inMemory;
            return this;
        }

        public DynamoDbConfigBuilder dbPath(@NonNull String dbPath) {
            Asserts.check(isValidDirectory(dbPath), "Directory is not valid.");
            Asserts.check(!inMemory, "You can't specify both -dbPath and -inMemory at once.");
            this.dbPath = dbPath;
            return this;
        }

        public DynamoDbConfigBuilder cors(@NonNull String cors) {
            this.cors = cors;
            return this;
        }

        private boolean isValidDirectory(final String path) {
            try {
                File file = new File(path);
                return file.isDirectory() && file.canWrite();
            } catch(Exception ex) {
                return false;
            }
        }

        public DynamoDbConfig build() {
            if (optimizeDbBeforeStartup && StringUtils.isEmpty(dbPath)) {
                throw new IllegalStateException("You also must specify -dbPath when you use 'optimizeDbBeforeStartup' parameter.");
            }
            return new DynamoDbConfig(port, inMemory, delayTransientStatuses, dbPath, sharedDb, cors,
                optimizeDbBeforeStartup);
        }

    }

}
