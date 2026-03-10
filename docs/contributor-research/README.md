# Testcontainers Java Contribution Research

## Goal

This note summarizes contribution candidates for a Spring / Java / Kotlin backend engineer targeting `testcontainers/testcontainers-java`.

The focus is not only "what is easy", but "what will look strong to backend hiring teams while still having a realistic merge path".

## Repository Health

- Upstream repository: `testcontainers/testcontainers-java`
- Local fork remote: `PreAgile/testcontainers-java`
- Current local branch: `main`
- Working tree state at review time: clean

Signals that the repository is actively maintained:

- `good first issue` labels are still in use
- merged PRs exist in late 2025 and early 2026
- recent human-merged PRs include:
  - `#11498` merged on `2026-02-26`
  - `#11223` merged on `2026-03-02`

## Local Codebase Areas That Matter

Relevant local paths:

- `docs/contributing.md`
- `docs/contributing_docs.md`
- `docs/modules/k6.md`
- `modules/k6/src/test/java/org/testcontainers/k6/K6ContainerTests.java`
- `modules/r2dbc/src/main/java/org/testcontainers/r2dbc/R2DBCDatabaseContainer.java`
- `modules/postgresql/src/main/java/org/testcontainers/postgresql/PostgreSQLR2DBCDatabaseContainer.java`
- `modules/mysql/src/main/java/org/testcontainers/mysql/MySQLR2DBCDatabaseContainer.java`
- `modules/mongodb/src/main/java/org/testcontainers/mongodb/MongoDBContainer.java`
- `modules/jdbc/src/main/java/org/testcontainers/jdbc/ContainerDatabaseDriver.java`

## Maintainer Preference Pattern

Observed pattern from recent merged PRs:

- small scope
- module-focused changes
- tests included for code changes
- low review burden

Examples:

- `#11498` changed 2 files and added a focused implementation plus test update
- `#11223` changed 3 files with a targeted compatibility fix

This strongly suggests that a first PR should avoid broad cross-cutting core changes.

## Candidate Ranking

### 1. Best first PR: `#8780`

Title:

- `[Enhancement]: Add example test to the K6 module docs`

Why this is the safest first merge:

- issue scope is clear
- user pain is already documented in the issue body
- implementation is mostly docs/example work
- it does not require design negotiation across multiple modules

Problem analysis:

- `docs/modules/k6.md` currently includes a code snippet from `K6ContainerTests`
- the docs show the API, but do not clearly present the "typical test shape" that users expect
- the issue author explicitly compared it unfavorably to quickstart-style docs

Root cause:

- documentation exposes a snippet, but not an opinionated onboarding flow
- discoverability is weaker than in other Testcontainers modules

Why this helps a Spring/backend profile:

- not as strong as a code PR, but excellent as a first merged foothold
- shows you can read the repo conventions and contribute cleanly

Recommended PR shape:

- keep it to docs and possibly example snippet framing
- do not over-design a new example project unless maintainers ask for it
- align the K6 docs with the style of existing quickstart/test integration pages

Merge probability:

- high

### 2. Best second PR: `#8797`

Title:

- `Add getR2dbcUrl helper method to JdbcDatabaseContainer`

Why this is the best backend-signaling PR:

- directly relevant to Spring, reactive stacks, database integration, and developer ergonomics
- stronger hiring signal than a docs PR
- maintainers already discussed acceptable design direction

Problem analysis:

- current R2DBC support exposes `getOptions(container)` methods
- users who want a concrete R2DBC URL string must construct it manually
- that hurts discoverability and ease of use

Important maintainer guidance already found in the issue:

- do not force this into a generic `JdbcDatabaseContainer` API immediately
- preferred direction is static `getR2dbcUrl(container)` helpers on each `R2DBCDatabaseContainer` implementation
- this was discussed explicitly by maintainers in the issue thread

Existing failed attempt:

- PR `#9569` attempted the feature
- maintainer immediately asked for tests
- contributor then got stuck on MSSQL-specific test issues
- PR was eventually closed

What that means:

- the feature itself is not rejected
- the implementation failed because the scope was too broad and the testing path got messy

Best way to revive it:

- comment on the issue first
- confirm that the agreed static-method design is still welcome
- explicitly propose a narrow first step
- avoid broad "support everything at once" unless maintainers request it

Safer implementation plan:

1. Start with the stable implementations that are easiest to test cleanly
2. Add tests alongside each helper
3. Leave tricky drivers such as MSSQL for follow-up if needed

Why this helps a Spring/backend profile:

- strongest fit for Spring / Java backend positioning
- good talking point for R2DBC, developer experience, and integration testing

Merge probability:

- medium to high if kept narrow
- medium or lower if implemented across too many drivers at once

### 3. High-value but risky: `#3066`

Title:

- `Support init scripts for MongoDBContainer without manually customizing the WaitStrategy`

Why it is attractive:

- strong backend signal
- involves lifecycle, initialization order, replica set handling, and wait strategy design

Problem analysis from issue and code:

- placing scripts under `/docker-entrypoint-initdb.d` can trigger MongoDB restart behavior
- local `MongoDBContainer` currently relies on `Wait.forLogMessage("(?i).*waiting for connections.*", 1)`
- that does not robustly cover the init-script + replica-set startup sequence

Root cause:

- container readiness assumptions do not fully match Mongo init script behavior
- `MongoDBContainer` also performs replica set initialization after start, which increases timing sensitivity

Why it is risky as a first PR:

- more design ambiguity
- more regression risk
- higher chance of maintainers asking for a reproducer-heavy discussion or broader design changes

Merge probability:

- medium at best for a newcomer first PR

### 4. Not recommended first: `#1537`

Title:

- `Support hikari data-source-properties in ContainerDatabaseDriver`

Why it looks easy but is not:

- the surface request is "just pass `Properties info` through"
- in practice it may affect:
  - JDBC property propagation
  - connection creation semantics
  - cache key behavior in `ContainerDatabaseDriver`

Root cause:

- `ContainerDatabaseDriver.connect(String url, Properties info)` does not currently flow those properties in a newcomer-safe way
- any fix in this path touches JDBC core behavior, not just one module

Why it is a poor first PR:

- too core
- too broad
- too easy to create subtle regressions

Merge probability:

- low for a first contribution

## Recommended Order

1. `#8780` K6 docs/example PR
2. `#8797` R2DBC helper PR
3. `#3066` MongoDB wait-strategy/init-script PR

## Suggested PR Positioning

### For `#8780`

PR message should emphasize:

- clearer onboarding
- quickstart-style discoverability
- alignment with existing docs conventions

### For `#8797`

PR message should emphasize:

- narrowed scope
- implementation following the already discussed issue design
- tests added for each touched helper
- no attempt to redesign the entire R2DBC abstraction

Suggested issue comment before coding:

> I would like to revive this using the static `getR2dbcUrl(container)` approach discussed above.  
> To keep the scope reviewable, I plan to add the helper with tests for the stable implementations first and leave any problematic drivers for a follow-up PR if needed.  
> Does that still match the expected direction?

## Practical Notes

- For docs contributions, follow `docs/contributing_docs.md`
- For code contributions, follow `docs/contributing.md`
- Run formatting and tests before opening a PR
- Keep each PR scoped to one issue and one clear problem
- For a newcomer, "small and mergeable" is more valuable than "ambitious but stalled"
