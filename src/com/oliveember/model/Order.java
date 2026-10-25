package com.oliveember.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of an order placed in the restaurant system.
 * Tracks order ID, line items, settled total, creation timestamp, and lifecycle status.
 */
public class Order {

    private final int orderId;
    private final List<OrderItem> items;
    private final int totalAmount;
    private final String createdAt;
    private OrderStatus status;

    public Order(int orderId, List<OrderItem> items, int totalAmount, String createdAt, OrderStatus status) {
        this.orderId = orderId;
        // Defensive copy to ensure items snapshot cannot be mutated externally
        this.items = new ArrayList<>(items);
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public int getItemCount() {
        int count = 0;
        for (OrderItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    @Override
    public String toString() {
        return "Order #" + orderId + " [" + status + "] - ₹" + totalAmount + " (" + createdAt + ")";
    }
}
