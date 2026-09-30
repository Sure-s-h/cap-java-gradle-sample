package com.releaseowl.sample.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.releaseowl.sample.logic.OrderValidator;

/**
 * BRANCH-ONLY VARIANT - do not merge this file into main.
 *
 * <p>On the {@code mixed-test-results} branch this class produces a deliberate
 * mix: two tests pass and four fail. Combined with {@code ignoreFailures} on the
 * test task, the build still reports success, so the Static Analysis stage runs
 * and SonarQube renders a partial result rather than an all-or-nothing one.
 *
 * <p>Expected outcome on this branch:
 *
 * <pre>
 *   30 tests, 4 failed, 26 passed   -&gt; build SUCCEEDS, Sonar shows the mix
 * </pre>
 *
 * <p>The four failures assert values that are deliberately wrong. They exist to
 * exercise how failures are counted and displayed downstream - they are not real
 * defects in {@link OrderValidator}.
 */
class MixedResultsProbeTest {

	// ---------------------------------------------------------------- passing

	@Test
	@DisplayName("passes: order total is quantity times unit price")
	void passingTotalIsCorrect() {
		assertEquals(new BigDecimal("59.97"), OrderValidator.totalPrice(3, new BigDecimal("19.99")));
	}

	@Test
	@DisplayName("passes: the maximum order quantity is 100")
	void passingMaximumQuantity() {
		assertEquals(100, OrderValidator.MAX_QUANTITY_PER_ORDER);
	}

	// ---------------------------------------------------------------- failing

	@Test
	@DisplayName("fails on purpose: wrong expected total")
	void failingWrongTotal() {
		assertEquals(new BigDecimal("60.00"), OrderValidator.totalPrice(3, new BigDecimal("19.99")),
				"Deliberate failure: 3 x 19.99 is 59.97, not 60.00");
	}

	@Test
	@DisplayName("fails on purpose: wrong maximum quantity")
	void failingWrongMaximum() {
		assertEquals(50, OrderValidator.MAX_QUANTITY_PER_ORDER,
				"Deliberate failure: the maximum is 100, not 50");
	}

	@Test
	@DisplayName("fails on purpose: wrong rounding scale")
	void failingWrongScale() {
		assertEquals(3, OrderValidator.totalPrice(1, new BigDecimal("1.005")).scale(),
				"Deliberate failure: totals are rounded to 2 decimals, not 3");
	}

	@Test
	@DisplayName("fails on purpose: impossible claim about zero quantity")
	void failingZeroQuantityAccepted() {
		boolean zeroIsAccepted;
		try {
			OrderValidator.validateQuantity(0);
			zeroIsAccepted = true;
		} catch (IllegalArgumentException e) {
			zeroIsAccepted = false;
		}
		assertTrue(zeroIsAccepted, "Deliberate failure: quantity 0 is correctly rejected");
	}
}
