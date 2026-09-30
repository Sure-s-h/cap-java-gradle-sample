package com.releaseowl.sample.pipeline;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * A switch for verifying that the build pipeline actually turns red on test
 * failures. It passes by default and fails on demand:
 *
 * <pre>
 *   ./gradlew clean test                          -&gt; BUILD SUCCESSFUL
 *   ./gradlew clean test -Dsample.failtest=true   -&gt; BUILD FAILED
 *   SAMPLE_FAILTEST=true ./gradlew clean test     -&gt; BUILD FAILED
 * </pre>
 *
 * The system property is forwarded into the test JVM by the test task in
 * build.gradle; the environment variable is inherited automatically.
 */
class PipelineFailureProbeTest {

	private static final String PROPERTY = "sample.failtest";
	private static final String ENV_VARIABLE = "SAMPLE_FAILTEST";

	private static boolean probeEnabled() {
		return Boolean.parseBoolean(System.getProperty(PROPERTY, "false"))
				|| Boolean.parseBoolean(System.getenv(ENV_VARIABLE));
	}

	@Test
	void failsOnlyWhenThePipelineProbeIsEnabled() {
		if (probeEnabled()) {
			fail("Deliberate failure: the pipeline failure probe is enabled via -D" + PROPERTY
					+ "=true or " + ENV_VARIABLE + "=true. The pipeline must report this build as FAILED "
					+ "and must not produce a deployable artifact.");
		}
	}
}
