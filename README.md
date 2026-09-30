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

2. **There is no code generation.** The `cds-maven-plugin` `generate` goal produces typed
   interfaces under `cds.gen.*`. Nothing equivalent exists for Gradle, so the event
   handler works against the untyped `EventContext` instead. The CAP runtime behaves
   identically — only compile-time typing is lost.

If your pipeline's "CAP Java + Gradle" support assumes an SAP-provided Gradle plugin,
it is assuming something that does not exist.

---

## Requirements

| Tool   | Version | Notes                                                    |
|--------|---------|----------------------------------------------------------|
| JDK    | 21+     | `options.release = 21` in `build.gradle`                 |
| Gradle | 9.8.0   | supplied by the wrapper — no Gradle needed on the agent  |
| Node   | none    | the build **downloads its own Node 22**; the agent needs none     |

The first build runs `npm install` and compiles the CDS model, so it **needs network
access**.

---

## Build and test

```bash
./gradlew clean test
```

Expected: **24 tests, BUILD SUCCESSFUL, exit code 0.**

```bash
./gradlew clean build      # full build incl. the executable jar
./gradlew bootRun          # run locally on http://localhost:8080
```

When running locally:

```bash
curl 'http://localhost:8080/odata/v4/OrderService/$metadata'
curl 'http://localhost:8080/odata/v4/OrderService/maxQuantityPerOrder()'
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
| Red, MTA | `SAMPLE_FAILTEST=true mbt build -p=cf` | **exit 1**, **no .mtar produced** |

All four have been executed and confirmed.

If your pipeline cannot inject an environment variable, use the **`pipeline-failure-check`**
branch instead. It is this same project with the probe hard-wired to fail, so pointing the
pipeline at that branch produces a red build with no configuration at all. It exists only
for this check and must never be merged into `main`.

Two triggers exist because a pipeline may invoke Gradle indirectly and be unable to
pass `-D`. The system property is forwarded into the test JVM by the `test` task in
`build.gradle`; the environment variable is inherited automatically.

---

## Why this project has an mta.yaml

A Gradle project needs no MTA descriptor. It is here because the pipeline runs Piper's
`mtaBuild` step against this project, and when no descriptor is present `mtaBuild`
*synthesises* one from the pipeline configuration. With `general.buildTool` set to `npm`,
that synthesised descriptor runs `npm install` and then `grunt`:

```
"mta.yaml" file not found in project sources
"mta.yaml" created.
building the "tutorial" module...
executing the "grunt" command...
Fatal error: Unable to find local grunt.
```

Gradle was never invoked and the wrapper was never touched.

The committed descriptor makes `mtaBuild` delegate to Gradle instead, and runs the tests
in `before-all` so a failing test blocks packaging:

| Scenario | Command | Result |
|---|---|---|
| Green | `mbt build -p=cf` | **exit 0**, .mtar containing the executable jar |
| Red | `SAMPLE_FAILTEST=true mbt build -p=cf` | **exit 1**, **no .mtar** |

Both confirmed locally.

It invokes `sh gradlew` rather than `./gradlew`, so it does not depend on the executable
bit surviving checkout.

**This is a workaround, not the right shape.** A Gradle project should be built by a
Gradle step, not by `mtaBuild`. If the pipeline claims to support "CAP Java with Gradle",
falling back to the npm/MTA path is a detection defect worth reporting.

---

## Pipeline configuration (Jenkins / Piper / ReleaseOwl)

Piper's `gradleExecuteBuild` step defaults to **`gradle:6-jdk11-alpine`** — Gradle 6,
JDK 11, and no Node. That image **cannot build this project**:

| Requirement | Needed | In the default image |
|---|---|---|
| Java | 21+ | 11 |
| Node | **22+** | absent |
| Gradle | 9.8.0 | 6 — but supplied by the wrapper, so this one does not matter |

Because the build downloads its own Node, the image only has to supply a **JDK 21**.
Both `gradle:8-jdk21` (used by `gradleExecuteBuild`) and the `devxci/mbtci-java21-*`
images work.

### Why Node is downloaded rather than taken from the image

Piper's `gradleExecuteBuild` runs `gradle` directly on `gradle:8-jdk21`, which has Gradle
and a JDK but **no Node**:

```
sh: 1: npm: not found
> Task :cdsInstall FAILED
```

No public image ships Gradle + JDK 21 + Node together, so relying on the image is a dead
end. The [node-gradle](https://github.com/node-gradle/gradle-node-plugin) plugin
downloads Node 22 into `build/nodejs/` instead — the same approach the Maven sample gets
from the `cds-maven-plugin` `install-node` goal.

This makes the build self-contained and lets `@sap/cds-dk` stay on 10.x, which requires
Node >=22 and would otherwise be unusable on a Node 20 image.

The agent must be able to reach `https://nodejs.org/dist`. If it mirrors Node internally,
override the base URL:

```bash
gradle test -PnodeDistBaseUrl=https://your-mirror/nodejs
```

Verified locally by building with Node removed from PATH entirely: `:nodeSetup` fetched
Node 22.12.0 and all 24 tests passed.

This repository deliberately ships **no `.pipeline/config.yml`**, so the build image stays
under the pipeline's control. Note that a committed `.pipeline/config.yml` would
*override* what the pipeline supplies — Piper ranks project configuration above custom
defaults.

---

## Project layout

```
build.gradle                     Gradle build, CDS tasks, test configuration
settings.gradle
mta.yaml                         makes Piper's mtaBuild delegate to Gradle
package.json                     @sap/cds-dk, the CDS compiler
srv/order-service.cds            OData V4 service: actions and functions only
srv/src/main/java/.../logic/     plain business logic (unit-testable)
srv/src/main/java/.../handlers/  CAP event handler, untyped EventContext
srv/src/test/java/               the test suite
gen/                             CDS compiler output (git-ignored)
```

`build/`, `gen/`, `.gradle/`, `node_modules/` and `mta_archives/` are build output and are
not committed.

### Three notes on build.gradle

- **Source roots are remapped.** CAP keeps the service module under `srv/`, so
  `sourceSets` points at `srv/src/main/java` rather than the Gradle default.
- **`gen/` is a second resource root.** In a non-Maven project `cds build --for java`
  writes CSN and EDMX under `gen/`, not into `srv/src/main/resources`. Without that
  second root the model never reaches the classpath and every OData endpoint returns
  **404 while the build still succeeds**. `duplicatesStrategy = EXCLUDE` handles
  `application.yaml`, which `cds build` copies into `gen/` as well.
- **The plain jar is disabled.** The Spring Boot plugin otherwise emits both a 60 MB
  executable jar and a 6 KB `-plain` jar, which makes a build-result glob ambiguous.

## What the tests cover

| Test class | Type | Tests | What it proves |
|---|---|---|---|
| `OrderValidatorTest` | plain JUnit 5, no Spring | 19 | business rules; runs in milliseconds |
| `OrderServiceODataTest` | `@SpringBootTest` + real HTTP | 4 | the CAP runtime and OData V4 endpoint |
| `PipelineFailureProbeTest` | plain JUnit 5 | 1 | the pipeline reports failures (opt-in) |

Nested and parameterized tests are included, so a report parser is exercised against
more than the simplest possible XML shape.
