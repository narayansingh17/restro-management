package com.oliveember.model;

/**
 * Lifecycle statuses for an order in the Olive & Ember system.
 *
 * An order progresses sequentially through:
 * PENDING (in kitchen queue) -> PREPARING (in active station) -> READY -> COMPLETED.
 */
public enum OrderStatus {
    PENDING,
    PREPARING,
    READY,
    COMPLETED
}
