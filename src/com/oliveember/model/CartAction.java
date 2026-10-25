package com.oliveember.model;

/**
 * Records an individual cart modification action to support LIFO single-step Undo.
 * Captures the affected menuItemId and its quantity prior to the change.
 */
public class CartAction {

    private final int menuItemId;
    private final int previousQuantity;

    public CartAction(int menuItemId, int previousQuantity) {
        this.menuItemId = menuItemId;
        this.previousQuantity = previousQuantity;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public int getPreviousQuantity() {
        return previousQuantity;
    }

    @Override
    public String toString() {
        return "CartAction(menuItemId=" + menuItemId + ", previousQty=" + previousQuantity + ")";
    }
}
