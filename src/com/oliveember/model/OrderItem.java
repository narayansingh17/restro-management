package com.oliveember.model;

/**
 * Snapshot of a menu item, quantity, unit price and line subtotal within an order.
 * Preserves the item name and unit price at the time of order placement.
 */
public class OrderItem {

    private final int menuItemId;
    private final String name;
    private final int unitPrice;
    private int quantity;

    public OrderItem(int menuItemId, String name, int unitPrice, int quantity) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public OrderItem(MenuItem item, int quantity) {
        this(item.getId(), item.getName(), item.getPrice(), quantity);
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public String getName() {
        return name;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getSubtotal() {
        return unitPrice * quantity;
    }

    @Override
    public String toString() {
        return name + " × " + quantity + " (₹" + getSubtotal() + ")";
    }
}
