---
name: change-module
description: Fix a bug in, add a feature to, or upgrade the default image of an existing Testcontainers for Java module or core, without breaking the public API. Use for changes under modules/<name>/ or core/.
---

# Fix, extend or upgrade an existing module

Read [`AGENTS.md`](../../../AGENTS.md) first.

## 1. Understand the current behaviour

- Find the container class in `modules/<name>/src/main/java/org/testcontainers/<name>/`. Check whether a deprecated twin exists in `org.testcontainers.containers`; a change usually belongs in the new package, and the deprecated class should keep working.
- Read the existing tests and the module docs page (`docs/modules/**/<name>.md`), including which codeinclude markers it uses.
- For a bug, write a failing test first that reproduces the reported behaviour.

## 2. Make the change

- Keep the change focused on the issue. Don't reformat unrelated code (Spotless will handle formatting).
- **Public API changes must be additive.** Add new methods or overloads. To replace something, deprecate it with `@Deprecated` and a Javadoc `@deprecated use {@link ...}` pointer. Don't change existing signatures, return types or visibility; `japicmp` in `check` fails on that.
- Wait strategy changes: keep the overall startup timeout as one budget (don't give each sub-wait the full timeout), and make sure the strategy works for every image tag the module claims to support.
- Validate new `withXxx(...)` arguments and fail with a clear message.
- Main code stays Java 8 compatible.

## 3. Upgrading the default or tested image version

- Users always pass an explicit image, so an "upgrade" usually means updating the tags used in tests and docs. Some database modules keep a `DEFAULT_TAG` used by their JDBC/R2DBC container providers; changing it changes behaviour for `jdbc:tc:` URL users, so call that out in the PR.
- Check the image's release notes for changed ports, env vars, startup log lines or health endpoints, and adjust the wait strategy if needed.
- If older versions behave differently, keep them working (branch on the parsed version only when unavoidable) and keep a test for the oldest supported behaviour where practical.
- Don't open PRs that only bump a client library version; Dependabot does that.

## 4. Verify

```bash
./gradlew spotlessApply
./gradlew :testcontainers-<name>:check     # or :testcontainers:check for core
```

If you changed core, also run the tests of one or two modules that depend on the changed behaviour.

## 5. Docs and PR

- Update the module docs page if behaviour or options changed; prefer codeinclude from tests.
- In the PR, describe the broken behaviour and how the change fixes it, and reference the issue (`Fixes #<number>`).
