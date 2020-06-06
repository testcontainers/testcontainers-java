# Database containers

## Overview

You might want to use Testcontainers' database support:

 * **Instead of H2 database for DAO unit tests that depend on database features that H2 doesn't emulate.** Testcontainers is not as performant as H2, but does give you the benefit of 100% database compatibility (since it runs a real DB inside of a container).
 * **Instead of a database running on the local machine or in a VM** for DAO unit tests or end-to-end integration tests that need a database to be present. In this context, the benefit of Testcontainers is that the database always starts in a known state, without any contamination between test runs or on developers' local machines.

!!! note
    Of course, it's still important to have as few tests that hit the database as possible, and make good use of mocks for components higher up the stack.

See [JDBC](./jdbc.md) and [R2DBC](./r2dbc.md) for information on how to use Testcontainers with SQL-like databases.