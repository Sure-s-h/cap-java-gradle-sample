# CAP Java Gradle Sample

A standalone [SAP CAP Java](https://cap.cloud.sap/docs/java/) application built with
**Gradle**, created to verify test discovery and execution in a build pipeline.

It is the Gradle counterpart to `cap-java-maven-sample`: the same stateless
`OrderService`, the same business logic, the same failure probe — so the two can be
compared directly, with only the build tool differing.

---

## Read this first: CAP Java does not officially support Gradle

SAP ships **`cds-maven-plugin` and no Gradle equivalent**. There is no
`cds-gradle-plugin`, and no SAP-published Gradle CAP sample. This project is therefore
**hand-rolled**, and differs from the Maven sample in two ways that matter:

1. **The CDS compiler is driven directly.** `build.gradle` defines `cdsInstall` and
   `cdsBuild` tasks that shell out to `npm` and `npx cds build --for java`.
   `processResources` depends on `cdsBuild`.

2. **There is no code generation.** `cds-maven-plugin`'s `generate` goal produces typed
   interfaces under `cds.gen.*`. Nothing equivalent exists for Gradle, so the event
   handler works against the untyped `EventContext` instead. The CAP runtime behaves
   identically — only compile-time typing is lost.

If your pipeline's "CAP Java + Gradle" support assumes an SAP-provided Gradle plugin,
it is assuming something that does not exist. Worth confirming with the developer what
their implementation actually detects.

---

## Requirements

| Tool   | Version | Notes                                                    |
|--------|---------|----------------------------------------------------------|
| JDK    | 21+     | `options.release = 21` in `build.gradle`                 |
| Gradle | 9.8.0   | supplied by the wrapper — no Gradle needed on the agent  |
| Node   | 20+     | `@sap/cds-dk` 10; the CDS compiler is a Node tool         |

The first build runs `npm install` and compiles the CDS model, so it **needs network
access**.

---

## Build and test

```bash
./gradlew clean test
```

Expected: **24 tests, BUILD SUCCESSFUL, exit code 0.**

```bash
./gradlew clean build      # full build incl. the Spring Boot jar
./gradlew bootRun          # run locally on http://localhost:8080
```

When running locally:

```bash
curl http://localhost:8080/odata/v4/OrderService/\$metadata
curl http://localhost:8080/odata/v4/OrderService/maxQuantityPerOrder\(\)
curl -X POST http://localhost:8080/odata/v4/OrderService/submitOrder \
     -H 'Content-Type: application/json' \
     -d '{"quantity":3,"unitPrice":19.99}'
```

The last call returns `totalPrice: 59.97`.

---

## Test results for the pipeline

Gradle writes JUnit XML to a **different location than Maven** — this is the single
most likely thing for a pipeline to get wrong when adding Gradle support:

```
build/test-results/test/TEST-*.xml     <- Gradle   (JUnit XML, consume these)
build/reports/tests/test/index.html    <- Gradle   (human-readable)

srv/target/surefire-reports/TEST-*.xml <- Maven, for comparison
```

Glob: `**/build/test-results/test/TEST-*.xml`

A pipeline that only looks for `**/surefire-reports/**` will report **zero tests** for a
Gradle project while still passing the build — a silent false green, and exactly the
failure mode this sample exists to expose.

---

## Verifying that failures turn the pipeline red

| Scenario | Command | Expected |
|---|---|---|
| Green | `./gradlew clean test` | 24 tests, 0 failures, **exit 0** |
| Red, property | `./gradlew clean test -Dsample.failtest=true` | 24 tests, **1 failure**, **exit 1** |
| Red, environment | `SAMPLE_FAILTEST=true ./gradlew clean test` | 24 tests, **1 failure**, **exit 1** |

All three have been executed and confirmed.

Two triggers exist because a pipeline may invoke Gradle indirectly and be unable to
pass `-D`. The system property is forwarded into the test JVM by the `test` task in
`build.gradle`; the environment variable is inherited automatically.

---

## Pipeline configuration (Jenkins / Piper / ReleaseOwl)

Piper's `gradleExecuteBuild` step defaults to **`gradle:6-jdk11-alpine`** — Gradle 6,
JDK 11, and no Node. That image **cannot build this project**:

| Requirement | Needed | In the default image |
|---|---|---|
| Java | 21+ | 11 |
| Node | 20+ | absent |
| Gradle | 9.8.0 | 6 — but supplied by the wrapper, so this one does not matter |

A working image needs only **JDK 21 and Node**; the wrapper provides Gradle itself.
`devxci/mbtci-java21-node22` satisfies both and is already used by the Maven sample.

This repository deliberately ships **no `.pipeline/config.yml`**, so the image stays
under the pipeline's control. Note that a committed `.pipeline/config.yml` would
*override* what the pipeline supplies — Piper ranks project configuration above custom
defaults.

---

## Project layout

```
build.gradle                     Gradle build, CDS tasks, test configuration
settings.gradle
package.json                     @sap/cds-dk, the CDS compiler
srv/order-service.cds            OData V4 service: actions and functions only
srv/src/main/java/.../logic/     plain business logic (unit-testable)
srv/src/main/java/.../handlers/  CAP event handler, untyped EventContext
srv/src/test/java/               the test suite
gen/                             CDS compiler output (git-ignored)
```

`build/`, `gen/`, `.gradle/` and `node_modules/` are build output and are not committed.

### Two notes on `build.gradle`

- **Source roots are remapped.** CAP keeps the service module under `srv/`, so
  `sourceSets` points at `srv/src/main/java` rather than the Gradle default.
- **`gen/` is a second resource root.** In a non-Maven project `cds build --for java`
  writes CSN and EDMX under `gen/`, not into `srv/src/main/resources`. Without that
  second root the model never reaches the classpath and every OData endpoint returns
  **404** while the build still succeeds. `duplicatesStrategy = EXCLUDE` handles
  `application.yaml`, which `cds build` copies into `gen/` as well.

## What the tests cover

| Test class | Type | Tests | What it proves |
|---|---|---|---|
| `OrderValidatorTest` | plain JUnit 5, no Spring | 19 | business rules; runs in milliseconds |
| `OrderServiceODataTest` | `@SpringBootTest` + real HTTP | 4 | the CAP runtime and OData V4 endpoint |
| `PipelineFailureProbeTest` | plain JUnit 5 | 1 | the pipeline reports failures (opt-in) |

Nested and parameterized tests are included, so a report parser is exercised against
more than the simplest possible XML shape.
