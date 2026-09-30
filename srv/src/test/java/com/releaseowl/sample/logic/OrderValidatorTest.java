package com.releaseowl.sample.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Plain unit tests - no Spring context, so they run in milliseconds.
 */
class OrderValidatorTest {

	@Nested
	@DisplayName("validateQuantity")
	class ValidateQuantity {

		@ParameterizedTest
		@ValueSource(ints = { 1, 5, 99, OrderValidator.MAX_QUANTITY_PER_ORDER })
		void acceptsQuantitiesWithinRange(int quantity) {
			OrderValidator.validateQuantity(quantity);
		}

		@Test
		void rejectsNullQuantity() {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.validateQuantity(null));
			assertEquals("Quantity is mandatory", e.getMessage());
		}

		@ParameterizedTest
		@ValueSource(ints = { 0, -1, -100 })
		void rejectsNonPositiveQuantity(int quantity) {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.validateQuantity(quantity));
			assertTrue(e.getMessage().contains("greater than 0"), e.getMessage());
		}

		@Test
		void rejectsQuantityAboveMaximum() {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.validateQuantity(OrderValidator.MAX_QUANTITY_PER_ORDER + 1));
			assertTrue(e.getMessage().contains("must not exceed"), e.getMessage());
		}
	}

	@Nested
	@DisplayName("validateUnitPrice")
	class ValidateUnitPrice {

		@Test
		void acceptsZeroAndPositivePrices() {
			OrderValidator.validateUnitPrice(BigDecimal.ZERO);
			OrderValidator.validateUnitPrice(new BigDecimal("19.99"));
		}

		@Test
		void rejectsNullPrice() {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.validateUnitPrice(null));
			assertEquals("Unit price is mandatory", e.getMessage());
		}

		@Test
		void rejectsNegativePrice() {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.validateUnitPrice(new BigDecimal("-0.01")));
			assertTrue(e.getMessage().contains("must not be negative"), e.getMessage());
		}
	}

	@Nested
	@DisplayName("totalPrice")
	class TotalPrice {

		@ParameterizedTest
		@CsvSource({
				"1, 10.00, 10.00",
				"3, 19.99, 59.97",
				"10, 0.00, 0.00",
				"7, 1.005, 7.04"
		})
		void multipliesQuantityByUnitPrice(int quantity, String unitPrice, String expected) {
			assertEquals(new BigDecimal(expected), OrderValidator.totalPrice(quantity, new BigDecimal(unitPrice)));
		}

		@Test
		void alwaysRoundsToTwoDecimals() {
			assertEquals(2, OrderValidator.totalPrice(3, new BigDecimal("1.111")).scale());
		}

		@Test
		void propagatesQuantityValidation() {
			assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.totalPrice(0, BigDecimal.TEN));
		}

		@Test
		void propagatesPriceValidation() {
			assertThrows(IllegalArgumentException.class,
					() -> OrderValidator.totalPrice(1, new BigDecimal("-1")));
		}
	}
}
