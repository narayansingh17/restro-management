package com.oliveember.dsa;

import com.oliveember.model.CartAction;

/**
 * Custom Singly Linked-List Stack implementation for cart Undo functionality.
 *
 * Implements strict Last-In, First-Out (LIFO) tracking without using built-in
 * collections like java.util.Stack, LinkedList, or Deque.
 *
 * All primary operations (push, pop, peek, isEmpty, size, clear) run in O(1) time.
 */
public class LinkedCartActionStack {

    /**
     * Internal singly-linked list node holding a CartAction payload and next pointer.
     */
    private static class Node {
        private final CartAction data;
        private Node next;

        private Node(CartAction data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node top; // Pointer to the most recently pushed action
    private int size; // Number of recorded undo actions on the stack

    public LinkedCartActionStack() {
        this.top = null;
        this.size = 0;
    }

    /**
     * Pushes a new cart modification action onto the top of the stack in O(1) time.
     *
     * @param action the cart action to push (must not be null)
     * @throws IllegalArgumentException if action is null
     */
    public void push(CartAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Cannot push null action to LinkedCartActionStack");
        }

        Node newNode = new Node(action);
        newNode.next = top;
        top = newNode;
        size++;
    }

    /**
     * Removes and returns the most recent cart action from the top of the stack in O(1) time.
     *
     * @return the popped CartAction, or null if the stack is empty
     */
    public CartAction pop() {
        if (top == null) {
            return null; // Safe empty stack handling
        }

        CartAction action = top.data;
        top = top.next;
        size--;
        return action;
    }

    /**
     * Inspects the most recent cart action at the top of the stack without removing it in O(1) time.
     *
     * @return the CartAction at the top, or null if the stack is empty
     */
    public CartAction peek() {
        if (top == null) {
            return null;
        }
        return top.data;
    }

    /**
     * Checks whether the stack is empty in O(1) time.
     *
     * @return true if the stack contains no actions, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of actions currently stored on the stack in O(1) time.
     *
     * @return the stack depth
     */
    public int size() {
        return size;
    }

    /**
     * Resets the stack by clearing the top pointer and size counter in O(1) time.
     * Enables safe reuse after order placement or cart clearing.
     */
    public void clear() {
        top = null;
        size = 0;
    }
}
