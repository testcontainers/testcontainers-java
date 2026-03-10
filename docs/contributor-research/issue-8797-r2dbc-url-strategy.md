# Issue #8797 Research: `getR2dbcUrl` Contribution Strategy

## Summary

This document records the analysis for Testcontainers Java issue `#8797`:

- issue: `Add getR2dbcUrl helper method to JdbcDatabaseContainer`
- target repository: `testcontainers/testcontainers-java`
- contribution goal: choose an implementation strategy with a high merge probability

This note is not just about "can this be coded?".
It is about:

- what the feature means
- why the issue exists
- what design direction maintainers already accepted
- what failed before
- how to narrow scope to maximize merge probability
- how this can be presented as a strong backend-oriented open source contribution

## What the issue is really about

Today, Testcontainers Java already supports R2DBC integration for several databases.
That support is mainly exposed through static methods like:

- `PostgreSQLR2DBCDatabaseContainer.getOptions(container)`
- `MySQLR2DBCDatabaseContainer.getOptions(container)`
- `MariaDBR2DBCDatabaseContainer.getOptions(container)`
- `MSSQLR2DBCDatabaseContainer.getOptions(container)`

Those methods return `ConnectionFactoryOptions`, which can then be used to build a connection factory.

The problem is discoverability and developer ergonomics.

For many users, especially Spring backend engineers working with reactive stacks, what they often want is not options composition but a concrete R2DBC connection URL string.
Something like:

- `r2dbc:postgresql://...`
- `r2dbc:mysql://...`
- `r2dbc:mariadb://...`

The issue exists because users can get a JDBC URL very easily through the container APIs, but there is no equally discoverable helper for the R2DBC URL.

## Why this matters conceptually

This is a small feature, but it sits at an important backend developer experience boundary.

In practice, backend engineers often need:

- the container lifecycle handled by Testcontainers
- a direct connection string for framework config
- a convenient bridge between container runtime values and application bootstrap

This is especially relevant in:

- Spring Boot reactive applications
- Micronaut test resources
- custom integration test setup for R2DBC-based services

So the core value of the issue is not "adding a string helper".
It is improving the usability and discoverability of R2DBC support in a real backend workflow.

## Current code structure

Relevant current implementation paths:

- `modules/r2dbc/src/main/java/org/testcontainers/r2dbc/R2DBCDatabaseContainer.java`
- `modules/postgresql/src/main/java/org/testcontainers/postgresql/PostgreSQLR2DBCDatabaseContainer.java`
- `modules/mysql/src/main/java/org/testcontainers/mysql/MySQLR2DBCDatabaseContainer.java`
- `modules/mariadb/src/main/java/org/testcontainers/mariadb/MariaDBR2DBCDatabaseContainer.java`
- `modules/mssqlserver/src/main/java/org/testcontainers/mssqlserver/MSSQLR2DBCDatabaseContainer.java`

There are also legacy compatibility classes under `org.testcontainers.containers` for the same database families:

- `modules/postgresql/src/main/java/org/testcontainers/containers/PostgreSQLR2DBCDatabaseContainer.java`
- `modules/mysql/src/main/java/org/testcontainers/containers/MySQLR2DBCDatabaseContainer.java`
- `modules/mariadb/src/main/java/org/testcontainers/containers/MariaDBR2DBCDatabaseContainer.java`
- `modules/mssqlserver/src/main/java/org/testcontainers/containers/MSSQLR2DBCDatabaseContainer.java`

This matters because a contribution here is not only about the modern package paths.
It also needs awareness of compatibility surface and duplicate implementation classes.

## Root cause of the issue

The root cause is not that Testcontainers lacks R2DBC support.
It already has that.

The real root cause is:

- R2DBC support is exposed through low-level option objects
- URL generation is left to users
- that makes common usage less discoverable
- the API feels less ergonomic than `getJdbcUrl()`

So the issue is about API discoverability and user ergonomics, not missing protocol support.

## Maintainer discussion and agreed direction

The issue discussion is highly valuable because it reduces design ambiguity.

Important maintainers involved in the discussion include:

- `eddumelendez`
- `kiview`

The most important conclusion from the thread is this:

- maintainers agreed that the safest current design is to add a new static `getR2dbcUrl(container)` method on each `R2DBCDatabaseContainer` implementation

Example direction discussed by maintainers:

```java
public static String getR2dbcUrl(MySQLContainer<?> container) {
    return ...;
}
```

Key point:

- they did **not** settle on immediately adding a generic method to `JdbcDatabaseContainer`
- they did **not** reject the feature
- they did agree the feature is useful
- the remaining question became implementation scope and testing

This is extremely important because it means the contribution is not fighting maintainers on product direction.
The contribution can follow a path they already explicitly accepted.

## What failed before

There was already a previous PR attempt:

- PR `#9569`
- title: `Add getR2dbcUrl Method on MariaDB, MySQL, PostgreSQL, MsSQL`

This is a key learning source.

What happened:

- the contributor implemented the feature for several databases
- maintainer requested tests
- contributor then hit MSSQL-specific testing issues
- the PR eventually closed without landing

This is the main signal for how to approach the issue successfully.

The feature itself was not rejected.
The previous attempt failed mainly because:

- the scope was broad
- tests became messy
- MSSQL created friction

So the lesson is clear:

- keep the feature narrow
- keep the test matrix simple
- avoid dragging the first PR into MSSQL-specific instability unless really necessary

## Recommended implementation strategy

### Best strategy for merge probability

Do **not** start with a generic `JdbcDatabaseContainer.getR2dbcUrl()`.

Do:

- add static `getR2dbcUrl(container)` methods to concrete `R2DBCDatabaseContainer` classes
- keep implementation explicit per database
- avoid trying to invent one universal builder abstraction in the first PR

### Recommended first scope

Best first scope:

- PostgreSQL
- MySQL
- MariaDB

Do not include MSSQL in the first PR unless there is a very strong reason.

### Why MSSQL should be deferred

There are multiple reasons to defer MSSQL:

1. previous attempt already got stuck there
2. current MSSQL R2DBC implementation contains a database-name-related TODO
3. SQL Server startup and readiness behavior adds review and test complexity
4. a newcomer first PR should avoid known friction when the feature can be landed incrementally

This does not mean MSSQL should never be supported.
It means MSSQL is a poor choice for the first narrow revival PR.

## Recommended code shape

For each selected implementation:

- keep `getOptions(container)` unchanged
- add `getR2dbcUrl(container)`
- derive the URL using the same effective values already used by `configure(options)`

The most important design principle is consistency with existing behavior:

- same host source
- same mapped port source
- same database name source where supported
- same username/password source where appropriate

The contribution should not try to redesign R2DBC integration.
It should only expose a simpler helper based on the already-existing configuration path.

## Testing strategy

Testing is the difference between a plausible PR and a mergeable PR here.

### Weak testing approach

Weak approach:

- only assert that a string equals an expected literal format

Why weak:

- it proves only formatting
- it does not prove that the generated URL is actually consumable

### Strong testing approach

Preferred approach:

- generate the URL through `getR2dbcUrl(container)`
- pass it into `ConnectionFactories.get(...)`
- run a real query through the resulting connection

This aligns with the existing R2DBC test philosophy already present in the repository.

Relevant test base:

- `modules/r2dbc/src/testFixtures/java/org/testcontainers/r2dbc/AbstractR2DBCDatabaseContainerTest.java`

That test fixture already covers:

- creating a connection factory
- opening a connection
- executing a query
- asserting a successful result

A strong contribution should reuse this testing style instead of inventing a detached unit-only test.

## Why this scope should merge better

The merge-friendly version of this feature has the following characteristics:

- small scope
- aligned with maintainer comments
- tests included
- no broad API redesign
- no cross-cutting core changes
- no known MSSQL trap in the first pass

This matches the general maintainer preference pattern seen in other merged contributions:

- narrowly scoped
- module-focused
- low review burden
- easy to reason about

## Risk analysis

### Low-risk parts

- PostgreSQL
- MySQL
- MariaDB

These are relatively low risk because:

- existing R2DBC support already works
- option extraction already exists
- implementation paths are straightforward
- test setup already exists

### Medium-risk parts

- adding support to both modern and compatibility package variants

This is not conceptually hard, but it is easy to overlook.
A PR that updates only one package family may be seen as incomplete.

### Higher-risk parts

- MSSQL
- generic interface or abstract-base redesign
- adding behavior in `JdbcDatabaseContainer`

These raise review load and increase the chance that a small feature turns into an API discussion.

## Is this a good PR target?

Yes, but only with the right scope.

### Good target if:

- implemented for PostgreSQL/MySQL/MariaDB first
- tests are included
- PR body explicitly references the issue discussion and accepted direction
- feature is framed as discoverability and developer ergonomics

### Bad target if:

- implemented across every database at once
- pushed as a generic framework redesign
- submitted without strong tests
- dragged into MSSQL complexity from day one

So this is a good second PR candidate, but it should be treated as a precision strike, not a broad cleanup.

## Suggested PR framing

The PR should be framed around:

- improving discoverability
- exposing a helper for an already-supported flow
- following the design discussed in `#8797`
- keeping the change intentionally narrow

Good framing language:

- users already rely on `getOptions(container)` today
- this adds a more discoverable helper for direct R2DBC URL usage
- the implementation follows the static-method approach discussed in the issue
- the initial scope is limited to the stable and straightforward implementations

Avoid framing it as:

- a new R2DBC architecture
- a universal abstraction improvement
- a broad parity project for all drivers in one PR

## How this helps a backend career story

This contribution can be presented as a strong backend-oriented OSS signal if the story is told correctly.

### Why it is good for a backend portfolio

It shows:

- reading and respecting existing architecture
- understanding framework integration pain points
- improving developer experience without overengineering
- translating maintainer discussion into an implementation plan
- writing tests for infrastructure-level integration code

This is more valuable than "I changed a string helper" if explained well.

### Strong interview narrative

A good narrative would be:

- I analyzed an open source issue where reactive database support existed but was not ergonomic enough for real backend usage.
- I studied the maintainers' design discussion and identified why a previous PR attempt stalled.
- I narrowed the feature scope to the highest-confidence implementations to improve merge probability.
- I designed the change to match existing integration behavior rather than introducing a new abstraction.
- I validated the feature at the integration level instead of only asserting string formatting.

This demonstrates backend maturity:

- API judgment
- test judgment
- contribution strategy
- practical prioritization under maintainership constraints

### What to emphasize publicly

When using this in a resume, interview, or portfolio, the best emphasis is:

- backend developer ergonomics
- R2DBC / reactive integration support
- open source collaboration through existing maintainer direction
- shipping a narrowly scoped, test-backed improvement

Do not oversell it as a huge infrastructure rewrite.
Sell it as a thoughtful, well-scoped contribution that improved usability in a real Java backend ecosystem.

## Recommended next step

The highest-probability execution path is:

1. comment on issue `#8797` with a narrow-scope proposal
2. implement `getR2dbcUrl(container)` for PostgreSQL, MySQL, and MariaDB
3. add integration-style tests that prove the generated URLs work
4. leave MSSQL for a follow-up PR if maintainers request it

## Final recommendation

Issue `#8797` is worth pursuing.

But the success condition is not simply "implement the feature".
The success condition is:

- choose the maintainer-approved shape
- avoid broad API changes
- avoid the MSSQL trap in the first pass
- prove the helper works with real connections

If done that way, this is a strong and credible second contribution target for a Java / Spring / Kotlin backend engineer building an open source track record in the Testcontainers ecosystem.
