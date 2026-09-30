package com.releaseowl.sample.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Plain business logic, deliberately free of Spring and CAP types so it can be
 * covered by fast unit tests that need no application context.
 */
public final class OrderValidator {

	public static final int MAX_QUANTITY_PER_ORDER = 100;

	private OrderValidator() {
	}

	/**
	 * @throws IllegalArgumentException if the quantity is not orderable
	 */
	public static void validateQuantity(Integer quantity) {
		if (quantity == null) {
			throw new IllegalArgumentException("Quantity is mandatory");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than 0, was " + quantity);
		}
		if (quantity > MAX_QUANTITY_PER_ORDER) {
			throw new IllegalArgumentException(
					"Quantity must not exceed " + MAX_QUANTITY_PER_ORDER + ", was " + quantity);
		}
	}

	/**
	 * @throws IllegalArgumentException if the price is missing or negative
	 */
	public static void validateUnitPrice(BigDecimal unitPrice) {
		if (unitPrice == null) {
			throw new IllegalArgumentException("Unit price is mandatory");
		}
		if (unitPrice.signum() < 0) {
			throw new IllegalArgumentException("Unit price must not be negative, was " + unitPrice);
		}
	}

	/**
	 * Calculates the order total, rounded to two decimal places.
	 *
	 * @throws IllegalArgumentException if quantity or unit price are invalid
	 */
	public static BigDecimal totalPrice(Integer quantity, BigDecimal unitPrice) {
		validateQuantity(quantity);
		validateUnitPrice(unitPrice);
		return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
	}
}
