package com.releaseowl.sample.pipeline;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * BRANCH-ONLY VARIANT - do not merge this file into main.
 *
 * <p>On the {@code pipeline-failure-check} branch this probe fails
 * unconditionally, with no property and no environment variable required. Point
 * the pipeline at this branch to verify that a test failure is reported as a
 * failed build.
 *
 * <p>Expected outcome on this branch:
 *
 * <pre>
 *   24 tests completed, 1 failed   -&gt; stage FAILED, exit code 1
 * </pre>
 *
 * <p>On {@code main} the same test passes by default and fails only when
 * {@code -Dsample.failtest=true} or {@code SAMPLE_FAILTEST=true} is set.
 */
class PipelineFailureProbeTest {

	@Test
	void failsAlwaysOnThePipelineFailureCheckBranch() {
		fail("Deliberate failure from the 'pipeline-failure-check' branch. "
				+ "The pipeline must report this build as FAILED. If this build is "
				+ "reported as successful, that is the defect this branch exists to catch.");
	}
}
