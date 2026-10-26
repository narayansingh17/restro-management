package com.oliveember.service;

import com.oliveember.dsa.LinkedCartActionStack;
import com.oliveember.dsa.LinkedOrderQueue;
import com.oliveember.model.CartAction;
import com.oliveember.model.MenuItem;
import com.oliveember.model.Order;
import com.oliveember.model.OrderItem;
import com.oliveember.model.OrderStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central in-memory business logic coordinator for Olive & Ember.
 *
 * Owns the fixed menu, active cart, custom Undo Stack, kitchen Queue, and order history.
 * All public methods are synchronized to safely handle concurrent HTTP handler threads.
 */
public class RestaurantService {

    // -----------------------------------------------------------------------
    // Fixed Menu Catalog (authoritative source of items and prices)
    // -----------------------------------------------------------------------
    private static final MenuItem[] MENU = {
        new MenuItem(1,  "Woodfired Margherita",     "Artisanal Pizzas", "San Marzano tomatoes, fresh buffalo mozzarella, fragrant sweet basil, extra virgin olive oil.", 380, "/images/margherita.jpg"),
        new MenuItem(2,  "Ember Roasted Flatbread",  "Artisanal Pizzas", "Charred bell peppers, zucchini ribbons, kalamata olives, crumbled feta cheese, dried oregano.", 360, "/images/flatbread.jpg"),
        new MenuItem(3,  "Truffle Mushroom Risotto", "Main Course",      "Slow-simmered Carnaroli rice, wild porcini mushrooms, black truffle oil, aged parmesan reggiano.", 420, "/images/risotto.jpg"),
        new MenuItem(4,  "Four Cheese Ravioli",      "Main Course",      "Handmade egg pasta filled with ricotta, fontina, gorgonzola in velvety sage brown butter.", 460, "/images/ravioli.jpg"),
        new MenuItem(5,  "Rosemary Garlic Focaccia", "Starters",         "Warm house-baked sourdough focaccia topped with sea salt, garden rosemary, and whipped herb butter.", 240, "/images/focaccia.jpg"),
        new MenuItem(6,  "Smoked Burrata Salad",     "Starters",         "Creamy burrata heart, heirloom baby tomatoes, wild rocket arugula, aged Modena balsamic reduction.", 340, "/images/burrata.jpg"),
        new MenuItem(7,  "Classic Tiramisu",         "Desserts",         "Espresso-dipped savoiardi ladyfingers, rich mascarpone mousse, dusted with dark cocoa powder.", 280, "/images/tiramisu.jpg"),
        new MenuItem(8,  "Warm Olive Oil Cake",      "Desserts",         "Infused with citrus zest and almond essence, served with Madagascar vanilla bean gelato.", 260, "/images/cake.jpg"),
        new MenuItem(9,  "Hibiscus Sparkler",        "Beverages",        "Cold-steeped wild hibiscus flowers, fresh lime juice, crushed mint sprigs, and soda fizz.", 180, "/images/hibiscus.jpg"),
        new MenuItem(10, "Cardamom Cold Brew",       "Beverages",        "Single-origin cold brew steeped 18 hours with green cardamom pods and raw cane sugar.", 200, "/images/coldbrew.jpg"),
    };

    // -----------------------------------------------------------------------
    // Mutable Session State
    // -----------------------------------------------------------------------

    // Active cart: menuItemId -> OrderItem (using LinkedHashMap to preserve insertion order)
    private final Map<Integer, OrderItem> cart = new LinkedHashMap<>();

    // Custom linked-list stack tracking quantity changes before each cart mutation
    private final LinkedCartActionStack undoStack = new LinkedCartActionStack();

    // Custom linked-list queue holding orders awaiting kitchen dispatch (FIFO)
    private final LinkedOrderQueue kitchenQueue = new LinkedOrderQueue();

    // Complete session order history (orders remain here even after being dequeued)
    private final List<Order> orderHistory = new ArrayList<>();

    // Single active order in preparation workflow (single-station constraint)
    private Order activeOrder = null;

    // Monotonically increasing order ID, starting at 101
    private int nextOrderId = 101;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // -----------------------------------------------------------------------
    // Menu access
    // -----------------------------------------------------------------------

    public synchronized MenuItem[] getMenu() {
        return MENU;
    }

    /** Finds a menu item by ID, or null if not found. */
    public synchronized MenuItem findMenuItem(int id) {
        for (MenuItem item : MENU) {
            if (item.getId() == id) return item;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Cart queries
    // -----------------------------------------------------------------------

    /** Returns an unmodifiable snapshot of the current cart lines. */
    public synchronized List<OrderItem> getCartItems() {
        return new ArrayList<>(cart.values()); // defensive copy
    }

    /** Sum of all line totals (unit price × quantity) for active cart lines. */
    public synchronized int getCartTotal() {
        int total = 0;
        for (OrderItem item : cart.values()) {
            total += item.getSubtotal();
        }
        return total;
    }

    /** Returns whether the undo stack has at least one action available. */
    public synchronized boolean isUndoAvailable() {
        return !undoStack.isEmpty();
    }

    public synchronized int getCartSize() {
        return cart.size();
    }

    // -----------------------------------------------------------------------
    // Cart mutations — each records previous qty to the stack before acting
    // -----------------------------------------------------------------------

    /**
     * Adds one unit of the given menu item to the cart.
     * Records the previous quantity to the undo stack before modifying.
     * @throws IllegalArgumentException if itemId is not on the menu
     */
    public synchronized void addItem(int itemId) {
        MenuItem menuItem = findMenuItem(itemId);
        if (menuItem == null) {
            throw new IllegalArgumentException("Menu item not found: " + itemId);
        }

        int prevQty = cart.containsKey(itemId) ? cart.get(itemId).getQuantity() : 0;
        undoStack.push(new CartAction(itemId, prevQty));

        if (cart.containsKey(itemId)) {
            cart.get(itemId).setQuantity(prevQty + 1);
        } else {
            cart.put(itemId, new OrderItem(menuItem, 1));
        }
    }

    /**
     * Decreases the quantity of the given item by one.
     * Records the previous quantity to the undo stack before modifying.
     * Removes the cart line if the resulting quantity would reach zero.
     * @throws IllegalArgumentException if itemId is not in the cart
     */
    public synchronized void decreaseItem(int itemId) {
        if (!cart.containsKey(itemId)) {
            throw new IllegalArgumentException("Item not in cart: " + itemId);
        }
        int prevQty = cart.get(itemId).getQuantity();
        undoStack.push(new CartAction(itemId, prevQty));

        if (prevQty <= 1) {
            cart.remove(itemId);
        } else {
            cart.get(itemId).setQuantity(prevQty - 1);
        }
    }

    /**
     * Removes an item line from the cart entirely.
     * Records the previous quantity to the undo stack before modifying.
     * @throws IllegalArgumentException if itemId is not in the cart
     */
    public synchronized void removeItem(int itemId) {
        if (!cart.containsKey(itemId)) {
            throw new IllegalArgumentException("Item not in cart: " + itemId);
        }
        int prevQty = cart.get(itemId).getQuantity();
        undoStack.push(new CartAction(itemId, prevQty));
        cart.remove(itemId);
    }

    /**
     * Pops the most recent CartAction and restores the item's previous quantity.
     * - If restored quantity is zero, the cart line is removed.
     * - This restoration does NOT push a new action (no undo of undo).
     * @return true if an action was undone, false if the stack was empty
     */
    public synchronized boolean undoLastAction() {
        CartAction action = undoStack.pop();
        if (action == null) {
            return false;
        }

        int itemId = action.getMenuItemId();
        int restoreQty = action.getPreviousQuantity();

        if (restoreQty <= 0) {
            cart.remove(itemId);
        } else {
            MenuItem menuItem = findMenuItem(itemId);
            if (menuItem == null) return true; // Edge case: item no longer in catalog; silently skip
            if (cart.containsKey(itemId)) {
                cart.get(itemId).setQuantity(restoreQty);
            } else {
                cart.put(itemId, new OrderItem(menuItem, restoreQty));
            }
        }
        return true;
    }

    /**
     * Clears all cart lines and resets the undo history.
     */
    public synchronized void clearCart() {
        cart.clear();
        undoStack.clear();
    }

    // -----------------------------------------------------------------------
    // Order placement
    // -----------------------------------------------------------------------

    /**
     * Validates the cart, creates an Order snapshot, assigns a sequential order ID,
     * records it in order history, and enqueues it in the kitchen queue.
     * Clears the cart and undo stack ONLY after a successful placement.
     *
     * @return the placed Order
     * @throws IllegalStateException if the cart is empty
     */
    public synchronized Order placeOrder() {
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot place an order with an empty cart.");
        }

        // Build a snapshot of current cart items preserving unit prices
        List<OrderItem> snapshot = new ArrayList<>();
        for (OrderItem line : cart.values()) {
            snapshot.add(new OrderItem(line.getMenuItemId(), line.getName(), line.getUnitPrice(), line.getQuantity()));
        }

        // Server calculates the authoritative total
        int total = 0;
        for (OrderItem line : snapshot) {
            total += line.getSubtotal();
        }

        String createdAt = LocalDateTime.now().format(TIME_FMT);
        Order order = new Order(nextOrderId, snapshot, total, createdAt, OrderStatus.PENDING);
        nextOrderId++;

        // Persist in order history (order stays here even after being dequeued from kitchen)
        orderHistory.add(order);

        // FIFO enqueue into the custom LinkedOrderQueue
        kitchenQueue.enqueue(order);

        // Clear cart and undo history only on success
        cart.clear();
        undoStack.clear();

        return order;
    }

    // -----------------------------------------------------------------------
    // Kitchen Workflow & Active Station State Machine (Phase 4)
    // -----------------------------------------------------------------------

    /** Returns the active order in preparation, or null if the station is idle. */
    public synchronized Order getActiveOrder() {
        return activeOrder;
    }

    /**
     * Dequeues the oldest pending order from the kitchen queue in O(1) time
     * and transitions its status to PREPARING as the active station order.
     *
     * Enforces strict single-order constraint: cannot start if an order is already active.
     * Enforces empty-queue check: cannot start if queue has no pending orders.
     *
     * @return the newly activated Order in PREPARING status
     * @throws IllegalStateException if an order is already active or the queue is empty
     */
    public synchronized Order startNextOrder() {
        if (activeOrder != null) {
            throw new IllegalStateException("Station busy: Order #" + activeOrder.getOrderId()
                + " is currently " + activeOrder.getStatus() + ". Only one order may be active at a time.");
        }
        if (kitchenQueue.isEmpty()) {
            throw new IllegalStateException("Kitchen queue is empty. No pending orders to start.");
        }

        Order nextOrder = kitchenQueue.dequeue();
        nextOrder.setStatus(OrderStatus.PREPARING);
        this.activeOrder = nextOrder;
        return nextOrder;
    }

    /**
     * Moves the current active order from PREPARING to READY status.
     *
     * @return the updated Order in READY status
     * @throws IllegalStateException if no active order or status is not PREPARING
     */
    public synchronized Order markActiveOrderReady() {
        if (activeOrder == null) {
            throw new IllegalStateException("No active order currently in preparation.");
        }
        if (activeOrder.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("Order #" + activeOrder.getOrderId()
                + " cannot transition to READY from " + activeOrder.getStatus() + ".");
        }

        activeOrder.setStatus(OrderStatus.READY);
        return activeOrder;
    }

    /**
     * Moves the current active order from READY to COMPLETED status and clears
     * the active station slot so the next pending order can be started.
     *
     * The completed order remains in orderHistory permanently for auditing and KPI sales totals.
     *
     * @return the completed Order
     * @throws IllegalStateException if no active order or status is not READY
     */
    public synchronized Order completeActiveOrder() {
        if (activeOrder == null) {
            throw new IllegalStateException("No active order to complete.");
        }
        if (activeOrder.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("Order #" + activeOrder.getOrderId()
                + " must be READY before completing (currently " + activeOrder.getStatus() + ").");
        }

        activeOrder.setStatus(OrderStatus.COMPLETED);
        Order completed = activeOrder;
        this.activeOrder = null;
        return completed;
    }

    /** Front order ID or null if kitchen queue is empty. */
    public synchronized Integer getFrontOrderId() {
        Order front = kitchenQueue.peek();
        return front != null ? front.getOrderId() : null;
    }

    /** Rear order ID or null if kitchen queue is empty. */
    public synchronized Integer getRearOrderId() {
        Order rear = kitchenQueue.peekRear();
        return rear != null ? rear.getOrderId() : null;
    }

    // -----------------------------------------------------------------------
    // Order History & Kitchen Queue Access
    // -----------------------------------------------------------------------

    /** Complete session order history (orders remain here even after being dequeued). */
    public synchronized List<Order> getOrderHistory() {
        return Collections.unmodifiableList(orderHistory);
    }

    public synchronized LinkedOrderQueue getKitchenQueue() {
        return kitchenQueue;
    }

    public synchronized List<Order> getKitchenQueueSnapshot() {
        return kitchenQueue.snapshotInOrder();
    }

    // -----------------------------------------------------------------------
    // Dashboard Statistics (Authoritative in-memory aggregation)
    // -----------------------------------------------------------------------

    public synchronized int getTotalOrdersPlaced() {
        return orderHistory.size();
    }

    public synchronized int getPendingOrderCount() {
        return kitchenQueue.size();
    }

    public synchronized int getPreparingOrderCount() {
        return (activeOrder != null && activeOrder.getStatus() == OrderStatus.PREPARING) ? 1 : 0;
    }

    public synchronized int getReadyOrderCount() {
        return (activeOrder != null && activeOrder.getStatus() == OrderStatus.READY) ? 1 : 0;
    }

    public synchronized int getCompletedOrderCount() {
        int count = 0;
        for (Order o : orderHistory) {
            if (o.getStatus() == OrderStatus.COMPLETED) count++;
        }
        return count;
    }

    /**
     * Authoritative sum of COMPLETED order amounts.
     * Strictly excludes pending, preparing, and ready orders.
     */
    public synchronized int getCompletedSales() {
        int sales = 0;
        for (Order o : orderHistory) {
            if (o.getStatus() == OrderStatus.COMPLETED) {
                sales += o.getTotalAmount();
            }
        }
        return sales;
    }

    /**
     * Returns up to {@code limit} most recent orders in reverse chronological order (newest first).
     */
    public synchronized List<Order> getRecentOrders(int limit) {
        List<Order> recent = new ArrayList<>();
        for (int i = orderHistory.size() - 1; i >= 0 && recent.size() < limit; i--) {
            recent.add(orderHistory.get(i));
        }
        return recent;
    }
}
