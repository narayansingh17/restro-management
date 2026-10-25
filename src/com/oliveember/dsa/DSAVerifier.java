package com.oliveember.dsa;

import com.oliveember.model.CartAction;
import com.oliveember.model.MenuItem;
import com.oliveember.model.Order;
import com.oliveember.model.OrderItem;
import com.oliveember.model.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone, dependency-free DSA Verification Test Runner for Olive & Ember.
 *
 * Validates the core algorithmic guarantees, edge cases, and pointer integrity
 * of LinkedOrderQueue (FIFO) and LinkedCartActionStack (LIFO).
 */
public class DSAVerifier {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println(" Olive & Ember — DSA Core Verification Suite (Phase 2)");
        System.out.println(" Testing LinkedOrderQueue (FIFO) & LinkedCartActionStack (LIFO)");
        System.out.println("================================================================\n");

        testLinkedOrderQueue();
        testLinkedCartActionStack();

        System.out.println("\n----------------------------------------------------------------");
        System.out.println(String.format(" VERIFICATION SUMMARY: %d Total | %d PASSED | %d FAILED",
                totalTests, passedTests, failedTests));
        System.out.println("----------------------------------------------------------------");

        if (failedTests > 0) {
            System.err.println("❌ Some DSA verification tests FAILED.");
            System.exit(1);
        } else {
            System.out.println("✅ All custom DSA data structures PASSED 100% of test assertions.");
        }
    }

    private static void testLinkedOrderQueue() {
        System.out.println("▶ TEST SUITE 1: LinkedOrderQueue (Custom Singly Linked List Queue)");

        LinkedOrderQueue queue = new LinkedOrderQueue();

        // 1. Initial Empty State
        assertTrue("Queue starts empty", queue.isEmpty());
        assertEqual("Queue starts with size 0", queue.size(), 0);
        assertNull("dequeue() on empty queue returns null safely", queue.dequeue());
        assertNull("peek() on empty queue returns null safely", queue.peek());

        // Helper mock order generator
        Order o101 = createMockOrder(101, 380, "14:00");
        Order o102 = createMockOrder(102, 420, "14:05");
        Order o103 = createMockOrder(103, 240, "14:10");

        // 2. Enqueue orders #101, #102, #103
        queue.enqueue(o101);
        assertEqual("Size after 1st enqueue", queue.size(), 1);
        assertFalse("Queue is not empty after 1st enqueue", queue.isEmpty());
        assertEqual("Peek returns front item #101", queue.peek().getOrderId(), 101);

        queue.enqueue(o102);
        assertEqual("Size after 2nd enqueue", queue.size(), 2);
        assertEqual("Peek still returns front item #101 after 2nd enqueue", queue.peek().getOrderId(), 101);

        queue.enqueue(o103);
        assertEqual("Size after 3rd enqueue", queue.size(), 3);
        assertEqual("Peek still returns front item #101 after 3rd enqueue", queue.peek().getOrderId(), 101);

        // 3. Verify snapshotInOrder() returns FIFO order without mutating queue
        List<Order> snapshot = queue.snapshotInOrder();
        assertEqual("Snapshot size matches queue size", snapshot.size(), 3);
        assertEqual("Snapshot element 0 is #101 (Front)", snapshot.get(0).getOrderId(), 101);
        assertEqual("Snapshot element 1 is #102", snapshot.get(1).getOrderId(), 102);
        assertEqual("Snapshot element 2 is #103 (Rear)", snapshot.get(2).getOrderId(), 103);
        assertEqual("Queue size unchanged by snapshot", queue.size(), 3);

        // 4. Confirm FIFO dequeue ordering
        Order d1 = queue.dequeue();
        assertNotNull("1st dequeue not null", d1);
        assertEqual("1st dequeue is oldest item #101 (FIFO)", d1.getOrderId(), 101);
        assertEqual("Size after 1st dequeue", queue.size(), 2);
        assertEqual("Peek now returns #102", queue.peek().getOrderId(), 102);

        Order d2 = queue.dequeue();
        assertNotNull("2nd dequeue not null", d2);
        assertEqual("2nd dequeue is #102 (FIFO)", d2.getOrderId(), 102);
        assertEqual("Size after 2nd dequeue", queue.size(), 1);
        assertEqual("Peek now returns #103", queue.peek().getOrderId(), 103);

        // 5. Dequeue final element: verify front and rear reset
        Order d3 = queue.dequeue();
        assertNotNull("3rd dequeue not null", d3);
        assertEqual("3rd dequeue is #103 (FIFO)", d3.getOrderId(), 103);
        assertEqual("Size is 0 after final dequeue", queue.size(), 0);
        assertTrue("Queue is empty after final dequeue", queue.isEmpty());
        assertNull("dequeue() on emptied queue returns null", queue.dequeue());
        assertNull("peek() on emptied queue returns null", queue.peek());

        // 6. Enqueue another order after queue becomes empty (tests reset pointers)
        Order o104 = createMockOrder(104, 760, "14:15");
        queue.enqueue(o104);
        assertEqual("Size after re-enqueue", queue.size(), 1);
        assertFalse("Queue is not empty after re-enqueue", queue.isEmpty());
        assertEqual("Peek returns new single element #104", queue.peek().getOrderId(), 104);

        Order d4 = queue.dequeue();
        assertEqual("Dequeued re-enqueued order is #104", d4.getOrderId(), 104);
        assertTrue("Queue is empty again", queue.isEmpty());
    }

    private static void testLinkedCartActionStack() {
        System.out.println("\n▶ TEST SUITE 2: LinkedCartActionStack (Custom Singly Linked List Stack)");

        LinkedCartActionStack stack = new LinkedCartActionStack();

        // 1. Initial Empty State
        assertTrue("Stack starts empty", stack.isEmpty());
        assertEqual("Stack starts with size 0", stack.size(), 0);
        assertNull("pop() on empty stack returns null safely", stack.pop());
        assertNull("peek() on empty stack returns null safely", stack.peek());

        // Mock Cart Actions: (menuItemId, previousQuantity)
        CartAction a1 = new CartAction(1, 0); // Adding item 1 (was 0)
        CartAction a2 = new CartAction(1, 1); // Incrementing item 1 (was 1)
        CartAction a3 = new CartAction(2, 0); // Adding item 2 (was 0)

        // 2. Push actions A1, A2, A3
        stack.push(a1);
        assertEqual("Size after 1st push", stack.size(), 1);
        assertFalse("Stack not empty after 1st push", stack.isEmpty());
        assertEqual("Peek returns most recent action A1", stack.peek().getMenuItemId(), 1);
        assertEqual("Peek action previousQty", stack.peek().getPreviousQuantity(), 0);

        stack.push(a2);
        assertEqual("Size after 2nd push", stack.size(), 2);
        assertEqual("Peek returns most recent action A2", stack.peek().getMenuItemId(), 1);
        assertEqual("Peek action previousQty", stack.peek().getPreviousQuantity(), 1);

        stack.push(a3);
        assertEqual("Size after 3rd push", stack.size(), 3);
        assertEqual("Peek returns most recent action A3", stack.peek().getMenuItemId(), 2);

        // 3. Confirm peek does not remove the element
        CartAction peekCheck = stack.peek();
        assertEqual("Peek returns A3", peekCheck.getMenuItemId(), 2);
        assertEqual("Size unchanged after peek", stack.size(), 3);

        // 4. Confirm LIFO pop ordering: A3 -> A2 -> A1
        CartAction p1 = stack.pop();
        assertNotNull("1st pop not null", p1);
        assertEqual("1st pop is most recent action A3 (LIFO)", p1.getMenuItemId(), 2);
        assertEqual("Size after 1st pop", stack.size(), 2);

        CartAction p2 = stack.pop();
        assertNotNull("2nd pop not null", p2);
        assertEqual("2nd pop is action A2 (LIFO)", p2.getMenuItemId(), 1);
        assertEqual("2nd pop previousQty is 1", p2.getPreviousQuantity(), 1);
        assertEqual("Size after 2nd pop", stack.size(), 1);

        CartAction p3 = stack.pop();
        assertNotNull("3rd pop not null", p3);
        assertEqual("3rd pop is initial action A1 (LIFO)", p3.getMenuItemId(), 1);
        assertEqual("3rd pop previousQty is 0", p3.getPreviousQuantity(), 0);
        assertEqual("Size after 3rd pop", stack.size(), 0);
        assertTrue("Stack is empty after popping all elements", stack.isEmpty());

        // 5. Verify empty pop and peek
        assertNull("pop() on emptied stack returns null", stack.pop());
        assertNull("peek() on emptied stack returns null", stack.peek());

        // 6. Verify reuse after becoming empty
        CartAction a4 = new CartAction(3, 2);
        stack.push(a4);
        assertEqual("Size after reuse push", stack.size(), 1);
        assertEqual("Pop reused item returns A4", stack.pop().getMenuItemId(), 3);
        assertTrue("Stack empty after popping reused item", stack.isEmpty());

        // 7. Verify clear() operation
        stack.push(new CartAction(4, 1));
        stack.push(new CartAction(5, 2));
        assertEqual("Size before clear()", stack.size(), 2);
        stack.clear();
        assertTrue("Stack isEmpty() is true after clear()", stack.isEmpty());
        assertEqual("Stack size() is 0 after clear()", stack.size(), 0);
        assertNull("pop() after clear() returns null", stack.pop());
    }

    private static Order createMockOrder(int id, int total, String time) {
        MenuItem item = new MenuItem(1, "Sample Item", "Mains", "Description", total);
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(item, 1));
        return new Order(id, items, total, time, OrderStatus.PENDING);
    }

    private static void assertTrue(String description, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  ✓ PASS: " + description);
        } else {
            failedTests++;
            System.err.println("  ✗ FAIL: " + description + " (expected true, got false)");
        }
    }

    private static void assertFalse(String description, boolean condition) {
        assertTrue(description, !condition);
    }

    private static void assertEqual(String description, Object actual, Object expected) {
        totalTests++;
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (match) {
            passedTests++;
            System.out.println("  ✓ PASS: " + description + " [" + actual + "]");
        } else {
            failedTests++;
            System.err.println("  ✗ FAIL: " + description + " (expected [" + expected + "], got [" + actual + "])");
        }
    }

    private static void assertNotNull(String description, Object obj) {
        assertTrue(description, obj != null);
    }

    private static void assertNull(String description, Object obj) {
        assertTrue(description, obj == null);
    }
}
