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

## Why this project has an mta.yaml

A Gradle project needs no MTA descriptor. It is here because the pipeline runs Piper's
**** step against this project, and when no  is present 
*synthesises* one from the pipeline configuration. With  that
synthesised descriptor runs 
up to date, audited 113 packages in 3s

33 packages are looking for funding
  run `npm fund` for details

found 0 vulnerabilities and then :



Gradle was never invoked. 
> Task :help

Welcome to Gradle 9.8.0.

To run a build, run gradlew <task> ...

To see a list of available tasks, run gradlew tasks

To see more detail about a task, run gradlew help --task <task>

To see a list of command-line options, run gradlew --help

For more detail on using Gradle, see https://docs.gradle.org/9.8.0/userguide/command_line_interface.html

For troubleshooting, visit https://help.gradle.org

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.0/userguide/configuration_cache_enabling.html was never touched.

The committed  makes  delegate to Gradle instead, and runs the tests
in  so a failing test blocks packaging:

| Scenario | Command | Result |
|---|---|---|
| Green | [2026-09-30 15:05:52]  INFO Cloud MTA Build Tool version 1.2.47
[2026-09-30 15:05:52]  INFO generating the "Makefile_20260930150552.mta" file...
[2026-09-30 15:05:52]  INFO done
[2026-09-30 15:05:52]  INFO executing the "make -f Makefile_20260930150552.mta p=cf mtar= strict=true mode=" command...
[2026-09-30 15:05:52]  INFO validating the MTA project
[2026-09-30 15:05:53]  INFO running the "before-all" build...
[2026-09-30 15:05:53]  INFO executing the "sh gradlew clean build" command...
..> Task :clean
.> Task :compileJava
> Task :cdsInstall UP-TO-DATE
> Task :cdsBuild UP-TO-DATE
> Task :processResources
> Task :classes
> Task :resolveMainClassName
..> Task :bootJar
> Task :jar SKIPPED
> Task :assemble
.> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
.......
> Task :test

OrderServiceODataTest > serviceExposesMetadata() PASSED

OrderServiceODataTest > submitOrderRejectsInvalidQuantityWithBadRequest() PASSED

OrderServiceODataTest > submitOrderReturnsTheCalculatedTotal() PASSED

OrderServiceODataTest > maxQuantityPerOrderFunctionIsCallable() PASSED

OrderValidatorTest > validateQuantity > rejectsNonPositiveQuantity(int) > [1] quantity = 0 PASSED

OrderValidatorTest > validateQuantity > rejectsNonPositiveQuantity(int) > [2] quantity = -1 PASSED

OrderValidatorTest > validateQuantity > rejectsNonPositiveQuantity(int) > [3] quantity = -100 PASSED

OrderValidatorTest > validateQuantity > rejectsQuantityAboveMaximum() PASSED

OrderValidatorTest > validateQuantity > acceptsQuantitiesWithinRange(int) > [1] quantity = 1 PASSED

OrderValidatorTest > validateQuantity > acceptsQuantitiesWithinRange(int) > [2] quantity = 5 PASSED

OrderValidatorTest > validateQuantity > acceptsQuantitiesWithinRange(int) > [3] quantity = 99 PASSED

OrderValidatorTest > validateQuantity > acceptsQuantitiesWithinRange(int) > [4] quantity = 100 PASSED

OrderValidatorTest > validateQuantity > rejectsNullQuantity() PASSED

OrderValidatorTest > totalPrice > alwaysRoundsToTwoDecimals() PASSED

OrderValidatorTest > totalPrice > propagatesPriceValidation() PASSED

OrderValidatorTest > totalPrice > propagatesQuantityValidation() PASSED

OrderValidatorTest > totalPrice > multipliesQuantityByUnitPrice(int, String, String) > [1] quantity = "1", unitPrice = "10.00", expected = "10.00" PASSED

OrderValidatorTest > totalPrice > multipliesQuantityByUnitPrice(int, String, String) > [2] quantity = "3", unitPrice = "19.99", expected = "59.97" PASSED

OrderValidatorTest > totalPrice > multipliesQuantityByUnitPrice(int, String, String) > [3] quantity = "10", unitPrice = "0.00", expected = "0.00" PASSED

OrderValidatorTest > totalPrice > multipliesQuantityByUnitPrice(int, String, String) > [4] quantity = "7", unitPrice = "1.005", expected = "7.04" PASSED

OrderValidatorTest > validateUnitPrice > rejectsNegativePrice() PASSED

OrderValidatorTest > validateUnitPrice > acceptsZeroAndPositivePrices() PASSED

OrderValidatorTest > validateUnitPrice > rejectsNullPrice() PASSED

PipelineFailureProbeTest > failsOnlyWhenThePipelineProbeIsEnabled() PASSED

2026-09-30T15:06:06.948+05:30  INFO 31012 --- [ionShutdownHook] o.s.boot.tomcat.GracefulShutdown         : Commencing graceful shutdown. Waiting for active requests to complete
.2026-09-30T15:06:06.957+05:30  INFO 31012 --- [tomcat-shutdown] o.s.boot.tomcat.GracefulShutdown         : Graceful shutdown complete

> Task :check
> Task :build

BUILD SUCCESSFUL in 13s
9 actionable tasks: 7 executed, 2 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.0/userguide/configuration_cache_enabling.html
[2026-09-30 15:06:07]  INFO validating the MTA project
[2026-09-30 15:06:07]  INFO building the "cap-java-gradle-sample-srv" module...
[2026-09-30 15:06:07]  INFO the build results of the "cap-java-gradle-sample-srv" module will be packaged and saved in the "C:SERSSURESH.TP-SAMPLESP-JAVA-GRADLE-SAMPLE.CAP-JAVA-GRADLE-SAMPLE_MTA_BUILD_TMPP-JAVA-GRADLE-SAMPLE-SRV" FOLDER
[2026-09-30 15:06:08]  INFO FINISHED BUILDING THE "CAP-JAVA-GRADLE-SAMPLE-SRV" MODULE
[2026-09-30 15:06:08]  INFO RUNNING THE "AFTER-ALL" BUILD...
[2026-09-30 15:06:08]  INFO GENERATING THE METADATA...
[2026-09-30 15:06:08]  INFO GENERATING THE "C:SERSSURESH.TP-SAMPLESP-JAVA-GRADLE-SAMPLE.CAP-JAVA-GRADLE-SAMPLE_MTA_BUILD_TMPMETA-INFMTAD.YAML" FILE...
[2026-09-30 15:06:08]  INFO GENERATING THE MTA ARCHIVE...
[2026-09-30 15:06:09]  INFO THE MTA ARCHIVE GENERATED AT: C:SERSSURESH.TP-SAMPLESP-JAVA-GRADLE-SAMPLEMTA_ARCHIVESP-JAVA-GRADLE-SAMPLE_1.0.0.MTAR
[2026-09-30 15:06:10]  INFO CLEANING TEMPORARY FILES... | **EXIT 0**,  PRODUCED CONTAINING THE EXECUTABLE JAR |
| RED | [2026-09-30 15:06:10]  INFO CLOUD MTA BUILD TOOL VERSION 1.2.47
[2026-09-30 15:06:10]  INFO GENERATING THE "MAKEFILE_20260930150610.MTA" FILE...
[2026-09-30 15:06:10]  INFO DONE
[2026-09-30 15:06:10]  INFO EXECUTING THE "MAKE -F MAKEFILE_20260930150610.MTA P=CF MTAR= STRICT=TRUE MODE=" COMMAND...
[2026-09-30 15:06:11]  INFO VALIDATING THE MTA PROJECT
[2026-09-30 15:06:11]  INFO RUNNING THE "BEFORE-ALL" BUILD...
[2026-09-30 15:06:11]  INFO EXECUTING THE "SH GRADLEW CLEAN BUILD" COMMAND...
.> TASK :CLEAN
.> TASK :COMPILEJAVA
> TASK :CDSINSTALL UP-TO-DATE
> TASK :CDSBUILD UP-TO-DATE
> TASK :PROCESSRESOURCES
> TASK :CLASSES
> TASK :RESOLVEMAINCLASSNAME
..> TASK :BOOTJAR
> TASK :JAR SKIPPED
> TASK :ASSEMBLE
.> TASK :COMPILETESTJAVA
> TASK :PROCESSTESTRESOURCES NO-SOURCE
> TASK :TESTCLASSES
......
> TASK :TEST

ORDERSERVICEODATATEST > SERVICEEXPOSESMETADATA() PASSED

ORDERSERVICEODATATEST > SUBMITORDERREJECTSINVALIDQUANTITYWITHBADREQUEST() PASSED

ORDERSERVICEODATATEST > SUBMITORDERRETURNSTHECALCULATEDTOTAL() PASSED

ORDERSERVICEODATATEST > MAXQUANTITYPERORDERFUNCTIONISCALLABLE() PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > REJECTSNONPOSITIVEQUANTITY(INT) > [1] QUANTITY = 0 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > REJECTSNONPOSITIVEQUANTITY(INT) > [2] QUANTITY = -1 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > REJECTSNONPOSITIVEQUANTITY(INT) > [3] QUANTITY = -100 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > REJECTSQUANTITYABOVEMAXIMUM() PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > ACCEPTSQUANTITIESWITHINRANGE(INT) > [1] QUANTITY = 1 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > ACCEPTSQUANTITIESWITHINRANGE(INT) > [2] QUANTITY = 5 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > ACCEPTSQUANTITIESWITHINRANGE(INT) > [3] QUANTITY = 99 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > ACCEPTSQUANTITIESWITHINRANGE(INT) > [4] QUANTITY = 100 PASSED

ORDERVALIDATORTEST > VALIDATEQUANTITY > REJECTSNULLQUANTITY() PASSED

ORDERVALIDATORTEST > TOTALPRICE > ALWAYSROUNDSTOTWODECIMALS() PASSED

ORDERVALIDATORTEST > TOTALPRICE > PROPAGATESPRICEVALIDATION() PASSED

ORDERVALIDATORTEST > TOTALPRICE > PROPAGATESQUANTITYVALIDATION() PASSED

ORDERVALIDATORTEST > TOTALPRICE > MULTIPLIESQUANTITYBYUNITPRICE(INT, STRING, STRING) > [1] QUANTITY = "1", UNITPRICE = "10.00", EXPECTED = "10.00" PASSED

ORDERVALIDATORTEST > TOTALPRICE > MULTIPLIESQUANTITYBYUNITPRICE(INT, STRING, STRING) > [2] QUANTITY = "3", UNITPRICE = "19.99", EXPECTED = "59.97" PASSED

ORDERVALIDATORTEST > TOTALPRICE > MULTIPLIESQUANTITYBYUNITPRICE(INT, STRING, STRING) > [3] QUANTITY = "10", UNITPRICE = "0.00", EXPECTED = "0.00" PASSED

ORDERVALIDATORTEST > TOTALPRICE > MULTIPLIESQUANTITYBYUNITPRICE(INT, STRING, STRING) > [4] QUANTITY = "7", UNITPRICE = "1.005", EXPECTED = "7.04" PASSED

ORDERVALIDATORTEST > VALIDATEUNITPRICE > REJECTSNEGATIVEPRICE() PASSED

ORDERVALIDATORTEST > VALIDATEUNITPRICE > ACCEPTSZEROANDPOSITIVEPRICES() PASSED

ORDERVALIDATORTEST > VALIDATEUNITPRICE > REJECTSNULLPRICE() PASSED

PIPELINEFAILUREPROBETEST > FAILSONLYWHENTHEPIPELINEPROBEISENABLED() FAILED
    ORG.OPENTEST4J.ASSERTIONFAILEDERROR AT PIPELINEFAILUREPROBETEST.JAVA:33

2026-09-30T15:06:22.923+05:30  INFO 2332 --- [IONSHUTDOWNHOOK] O.S.BOOT.TOMCAT.GRACEFULSHUTDOWN         : COMMENCING GRACEFUL SHUTDOWN. WAITING FOR ACTIVE REQUESTS TO COMPLETE
2026-09-30T15:06:22.944+05:30  INFO 2332 --- [TOMCAT-SHUTDOWN] O.S.BOOT.TOMCAT.GRACEFULSHUTDOWN         : GRACEFUL SHUTDOWN COMPLETE
.
> TASK :TEST FAILED
9 ACTIONABLE TASKS: 7 EXECUTED, 2 UP-TO-DATE
[2026-09-30 15:06:23] ERROR THE "BEFORE-ALL"" BUILD FAILED: COULD NOT EXECUTE THE "SH GRADLEW CLEAN BUILD" COMMAND: EXIT STATUS 1
[2026-09-30 15:06:23] ERROR COULD NOT BUILD THE MTA PROJECT: COULD NOT EXECUTE THE "MAKE -F MAKEFILE_20260930150610.MTA P=CF MTAR= STRICT=TRUE MODE=" COMMAND: EXIT STATUS 2 | **EXIT 1**, **NO ** |

BOTH CONFIRMED LOCALLY.

**THIS IS A WORKAROUND, NOT THE RIGHT SHAPE.** A GRADLE PROJECT SHOULD BE BUILT BY A
GRADLE STEP, NOT BY . IF THE PIPELINE CLAIMS TO SUPPORT "CAP JAVA WITH GRADLE",
FALLING BACK TO THE NPM/MTA PATH IS A DETECTION DEFECT WORTH REPORTING.

---

## PIPELINE CONFIGURATION (JENKINS / PIPER / RELEASEOWL)

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
