package org.testcontainers.r2dbc;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.r2dbc.client.R2dbc;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Result;
import lombok.NonNull;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import reactor.core.publisher.Flux;

import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Base class for containers that expose a R2DBC connection
 *
 * @author humblehound
 */
public abstract class R2dbcDatabaseContainer<SELF extends R2dbcDatabaseContainer<SELF>> extends GenericContainer<SELF> {

    private int startupTimeoutSeconds = 120;
    private R2dbc r2dbc;

    public R2dbcDatabaseContainer(@NonNull final String dockerImageName) {
        super(dockerImageName);
    }

    public R2dbcDatabaseContainer(@NonNull final Future<String> image) {
        super(image);
    }

    public abstract R2dbcConnectionParams getR2dbcConnectionParams();

    /**
     * @return a test query string suitable for testing that this particular database type is alive
     */
    protected abstract String getTestQueryString();

    public synchronized R2dbc getR2dbc() {
        if (r2dbc == null) {
            r2dbc = new R2dbc(createConnection(getR2dbcConnectionParams()));
            return r2dbc;
        } else {
            return r2dbc;
        }
    }

    @Override
    protected void waitUntilContainerStarted() {
        // Repeatedly try and open a connection to the DB and execute a test query

        logger().info("Waiting for database connection to become available at {} using query '{}'", "", getTestQueryString());
        Unreliables.retryUntilSuccess(startupTimeoutSeconds, TimeUnit.SECONDS, () -> {

            if (!isRunning()) {
                throw new ContainerLaunchException("Container failed to start");
            }

            R2dbc r2dbc = getR2dbc();

            int success = r2dbc.inTransaction(handle ->
                handle.createQuery(this.getTestQueryString()).mapResult(Result::getRowsUpdated)
            ).blockFirst();

            if (success > 0) {
                logger().info("Obtained a connection to container ({})", this.getTestQueryString());
                return null;
            } else {
                throw new SQLException("Failed to execute test query");
            }

        });
    }


    public PostgresqlConnectionFactory createConnection(R2dbcConnectionParams r2dbcConnectionParams) {
        PostgresqlConnectionConfiguration config = PostgresqlConnectionConfiguration
            .builder()
            .host(r2dbcConnectionParams.getHost())
            .port(r2dbcConnectionParams.getPort())
            .database(r2dbcConnectionParams.getDatabase())
            .username(r2dbcConnectionParams.getUsername())
            .password(r2dbcConnectionParams.getPassword()).build();
        return new PostgresqlConnectionFactory(config);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        runInitScriptIfRequired();
    }

    /**
     * Load init script content and apply it to the database if initScriptPath is set
     */
    protected void runInitScriptIfRequired() {
//        if (initScriptPath != null) {
//            ScriptUtils.runInitScript(getDatabaseDelegate(), initScriptPath);
//        }
    }
}
