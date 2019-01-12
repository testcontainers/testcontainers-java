package org.testcontainers.r2dbc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class R2dbcConnectionParams {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
}
