/**
 * A stateless CAP service: actions and functions only, no entities.
 * With no persistent entities there is no database, so the deployed
 * application needs no service bindings.
 */
service OrderService {

  type OrderConfirmation {
    accepted   : Boolean;
    quantity   : Integer;
    unitPrice  : Decimal(9,2);
    totalPrice : Decimal(9,2);
  }

  /** Validates an order and returns the calculated totals. */
  action submitOrder(quantity : Integer, unitPrice : Decimal(9,2)) returns OrderConfirmation;

  /** The largest quantity a single order may contain. */
  function maxQuantityPerOrder() returns Integer;
}
