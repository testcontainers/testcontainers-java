---
name: add-module
description: Add a new Testcontainers for Java module for a technology (new container class, tests, docs and repo registration). Use when asked to create or contribute a new module under modules/.
---

# Add a new module

Read [`AGENTS.md`](../../../AGENTS.md) first; this playbook only adds the module-specific steps.

## 0. Check it should be a module

New modules need maintainer buy-in before significant work (see "Contributing new modules" in
[`docs/contributing.md`](../../../docs/contributing.md)). Confirm with the person you are working for that:

- there is an issue or discussion where maintainers agreed to the module, and
- it adds value beyond a `GenericContainer` snippet (technology-specific wait strategy, non-trivial setup, client helpers).

If not, suggest an example under `examples/` or a third-party library instead, and stop.

Check the default image too: an official Docker Hub image or one published by the vendor, with a public Dockerfile, immutable version tags, and a license that lets anyone pull and use it.

## 1. Create the Gradle project

`settings.gradle` includes every directory under `modules/` automatically as `:testcontainers-<name>`.

```
modules/<name>/
  build.gradle
  src/main/java/org/testcontainers/<name>/<Name>Container.java
  src/test/java/org/testcontainers/<name>/<Name>ContainerTest.java
  src/test/resources/logback-test.xml   # copy from an existing module
```

Minimal `build.gradle` (see `modules/typesense/build.gradle`):

```groovy
description = "Testcontainers :: <Display Name>"

dependencies {
    api project(':testcontainers')

    // compileOnly 'vendor:client:x.y.z'      // optional, for internal use in main code; never expose its types in public API
    testImplementation 'vendor:client:x.y.z'
}
```

## 2. Write the container class

Use `modules/typesense/src/main/java/org/testcontainers/typesense/TypesenseContainer.java` as the reference shape:

- `DEFAULT_IMAGE_NAME = DockerImageName.parse("<vendor>/<image>")` without a tag.
- Constructors `(String)` and `(DockerImageName)`; the second calls `dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME)`.
- `withExposedPorts(...)` and a technology-specific `waitingFor(...)`.
- Configuration via `withXxx(...)` methods returning `this`; validate inputs. Apply env/commands in `configure()` when they depend on settings.
- Getters for everything a client needs (endpoint URL, mapped ports, credentials).
- Class Javadoc listing the supported image and exposed ports.
- Java 8 source level, JDK types only in public API.

## 3. Write tests

- Pin an explicit image tag, e.g. `new <Name>Container("<vendor>/<image>:1.2.3")`.
- Cover default startup with a real client call and each `withXxx` option.
- Wrap the snippet you want in the docs with `// container {` and `// }`.

Run:

```bash
./gradlew spotlessApply
./gradlew :testcontainers-<name>:check
```

`japicmp` is skipped automatically for a module that has no released version yet.

## 4. Documentation

- Create `docs/modules/<name>.md` (or `docs/modules/databases/<name>.md`), following `docs/modules/typesense.md`: overview, codeinclude usage example, dependency block with `{{latest_version}}`.
- Add the incubating note at the top (text in `docs/contributing.md`, "Incubating modules").
- Add the page to the `nav` in `mkdocs.yml`, keeping alphabetical order within its section.

## 5. Register the module in repo metadata

Add the module, keeping alphabetical order, to:

- [ ] `.github/ISSUE_TEMPLATE/bug_report.yaml` (module dropdown)
- [ ] `.github/ISSUE_TEMPLATE/enhancement.yaml`
- [ ] `.github/ISSUE_TEMPLATE/feature.yaml`
- [ ] `.github/dependabot.yml`: add `/modules/<name>` to the "Modules without additional compatibility constraints" entry, unless it needs its own ignore rules
- [ ] `.github/labeler.yml`: a `modules/<name>` label matching `modules/<name>/**/*`

## 6. Open the PR

Paste the "Checklist" from `docs/contributing.md` into the PR description and tick what applies. Link the issue or discussion where the module was agreed.
