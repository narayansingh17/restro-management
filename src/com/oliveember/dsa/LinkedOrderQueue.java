package com.oliveember.dsa;

import com.oliveember.model.Order;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Singly Linked-List Queue implementation for kitchen FIFO order processing.
 *
 * Implements strict First-In, First-Out (FIFO) queuing without using built-in
 * collections like java.util.Queue, LinkedList, or ArrayDeque.
 *
 * All primary operations (enqueue, dequeue, peek, isEmpty, size) run in O(1) time.
 */
public class LinkedOrderQueue {

    /**
     * Internal singly-linked list node holding an Order payload and next pointer.
     */
    private static class Node {
        private final Order data;
        private Node next;

        private Node(Order data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node front; // Head of the queue: orders are dequeued from here
    private Node rear;  // Tail of the queue: new orders are enqueued here
    private int size;   // Number of active pending orders in the queue

    public LinkedOrderQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    /**
     * Enqueues an order at the rear of the queue in O(1) time.
     *
     * @param order the order to append to the queue (must not be null)
     * @throws IllegalArgumentException if order is null
     */
    public void enqueue(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Cannot enqueue null order into LinkedOrderQueue");
        }

        Node newNode = new Node(order);
        if (rear == null) {
            // Queue was previously empty; front and rear both point to the new node
            front = newNode;
            rear = newNode;
        } else {
            // Append after current rear and advance rear pointer
            rear.next = newNode;
            rear = newNode;
        }
        size++;
    }

    /**
     * Removes and returns the oldest order from the front of the queue in O(1) time.
     *
     * If the queue becomes empty after this removal, both front and rear pointers
     * are cleanly reset to null, enabling safe future enqueues.
     *
     * @return the dequeued Order, or null if the queue is empty
     */
    public Order dequeue() {
        if (front == null) {
            return null; // Safe empty queue handling
        }

        Order dequeuedOrder = front.data;
        front = front.next;
        size--;

        // If the queue is now empty, reset rear pointer as well
        if (front == null) {
            rear = null;
        }

        return dequeuedOrder;
    }

    /**
     * Inspects the oldest order at the front of the queue without removing it in O(1) time.
     *
     * @return the Order at the front, or null if the queue is empty
     */
    public Order peek() {
        if (front == null) {
            return null;
        }
        return front.data;
    }

    /**
     * Inspects the most recently added order at the rear of the queue in O(1) time.
     *
     * @return the Order at the rear, or null if the queue is empty
     */
    public Order peekRear() {
        if (rear == null) {
            return null;
        }
        return rear.data;
    }

    /**
     * Checks whether the queue contains zero orders in O(1) time.
     *
     * @return true if the queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the current number of orders in the queue in O(1) time.
     *
     * @return the current queue depth
     */
    public int size() {
        return size;
    }

    /**
     * Creates and returns a new ArrayList containing the pending orders in FIFO order,
     * without exposing internal Node references and without mutating the queue.
     *
     * Traversal is O(n) where n is the number of queued elements.
     *
     * @return an ordered snapshot list of pending orders (from front to rear)
     */
    public List<Order> snapshotInOrder() {
        List<Order> snapshot = new ArrayList<>(size);
        Node current = front;
        while (current != null) {
            snapshot.add(current.data);
            current = current.next;
        }
        return snapshot;
    }
}
