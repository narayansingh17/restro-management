package com.oliveember;

import com.oliveember.model.MenuItem;
import com.oliveember.model.Order;
import com.oliveember.model.OrderItem;
import com.oliveember.service.RestaurantService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Main application entry point for Olive & Ember — Restaurant Order Management System.
 *
 * Runs a Java 17 built-in HttpServer on localhost:8080 that serves:
 *  - Static files from frontend/
 *  - JSON REST API endpoints under /api/
 */
public class Main {

    private static final int PORT = 8080;
    private static final String HOST = "127.0.0.1";
    private static final Path FRONTEND_DIR = Path.of("frontend").toAbsolutePath().normalize();

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm",  "text/html; charset=UTF-8");
        MIME_TYPES.put("css",  "text/css; charset=UTF-8");
        MIME_TYPES.put("js",   "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("svg",  "image/svg+xml");
        MIME_TYPES.put("png",  "image/png");
        MIME_TYPES.put("jpg",  "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("gif",  "image/gif");
        MIME_TYPES.put("ico",  "image/x-icon");
        MIME_TYPES.put("txt",  "text/plain; charset=UTF-8");
    }

    public static void main(String[] args) throws IOException {
        if (!Files.exists(FRONTEND_DIR) || !Files.isDirectory(FRONTEND_DIR)) {
            System.err.println("Warning: 'frontend' directory not found at " + FRONTEND_DIR);
        }

        // Shared singleton service — owns all business logic and DSA state
        RestaurantService service = new RestaurantService();

        // Use a small fixed thread pool so concurrent requests don't block each other
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 16);
        server.setExecutor(Executors.newFixedThreadPool(4));

        // API contexts — registered first so they take priority over the static handler
        server.createContext("/api/menu",                new MenuHandler(service));
        server.createContext("/api/cart",                new CartHandler(service));
        server.createContext("/api/cart/items/add",      new AddItemHandler(service));
        server.createContext("/api/cart/items/decrease", new DecreaseItemHandler(service));
        server.createContext("/api/cart/items/remove",   new RemoveItemHandler(service));
        server.createContext("/api/cart/undo",           new CartUndoHandler(service));
        server.createContext("/api/cart/clear",          new CartClearHandler(service));

        // Orders & History (GET = history list, POST = place order)
        OrdersHandler ordersHandler = new OrdersHandler(service);
        server.createContext("/api/orders",              ordersHandler);
        server.createContext("/api/history",             ordersHandler);

        // Kitchen Workflow & Queue
        KitchenHandler kitchenHandler = new KitchenHandler(service);
        server.createContext("/api/kitchen",             kitchenHandler);
        server.createContext("/api/kitchen/queue",       kitchenHandler);
        KitchenStartNextHandler startHandler = new KitchenStartNextHandler(service);
        server.createContext("/api/kitchen/start-next",  startHandler);
        server.createContext("/api/kitchen/start",       startHandler);
        KitchenMarkReadyHandler readyHandler = new KitchenMarkReadyHandler(service);
        server.createContext("/api/kitchen/mark-ready",  readyHandler);
        server.createContext("/api/kitchen/ready",       readyHandler);
        server.createContext("/api/kitchen/complete",    new KitchenCompleteHandler(service));

        // Dashboard Aggregated KPIs
        server.createContext("/api/dashboard",          new DashboardHandler(service));

        // Static file handler catches all remaining paths
        server.createContext("/", new StaticFileHandler());

        server.start();

        String url = "http://" + HOST + ":" + PORT + "/";
        System.out.println("==================================================");
        System.out.println(" Olive & Ember — Restaurant Order Management System");
        System.out.println(" Java 17 Backend Server Active (Phase 4)");
        System.out.println(" URL: " + url);
        System.out.println(" Serving frontend from: " + FRONTEND_DIR);
        System.out.println("==================================================");

        openBrowser(url);
    }

    // -----------------------------------------------------------------------
    // Shared API utilities
    // -----------------------------------------------------------------------

    /** Reads form-urlencoded POST body and parses fields into a Map. */
    static Map<String, String> parseFormBody(HttpExchange exchange) throws IOException {
        Map<String, String> fields = new HashMap<>();
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (body.isBlank()) return fields;
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    fields.put(decode(kv[0]), decode(kv[1]));
                } else if (kv.length == 1) {
                    fields.put(decode(kv[0]), "");
                }
            }
        }
        return fields;
    }

    private static String decode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    /** Sends a JSON response body with the given HTTP status code. */
    static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Builds a simple { "error": "..." } JSON string. Escapes the message safely. */
    static String jsonError(String message) {
        return "{\"error\":\"" + escapeJson(message) + "\"}";
    }

    /** Minimally safe JSON string character escaping (no external library needed). */
    static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Builds JSON representing a single cart line. */
    static String orderItemToJson(OrderItem item) {
        return "{\"menuItemId\":" + item.getMenuItemId()
             + ",\"name\":\"" + escapeJson(item.getName()) + "\""
             + ",\"unitPrice\":" + item.getUnitPrice()
             + ",\"quantity\":" + item.getQuantity()
             + ",\"subtotal\":" + item.getSubtotal()
             + "}";
    }

    /** Builds a full JSON cart payload (items array, total, undoAvailable). */
    static String cartToJson(RestaurantService service) {
        List<OrderItem> items = service.getCartItems();
        int total = service.getCartTotal();
        boolean undoAvail = service.isUndoAvailable();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(orderItemToJson(items.get(i)));
        }
        sb.append("],\"total\":").append(total);
        sb.append(",\"undoAvailable\":").append(undoAvail);
        sb.append("}");
        return sb.toString();
    }

    /** Builds JSON representing a complete Order snapshot. */
    static String orderToJson(Order order) {
        if (order == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"orderId\":").append(order.getOrderId())
          .append(",\"total\":").append(order.getTotalAmount())
          .append(",\"totalAmount\":").append(order.getTotalAmount())
          .append(",\"createdAt\":\"").append(escapeJson(order.getCreatedAt())).append("\"")
          .append(",\"status\":\"").append(order.getStatus().name()).append("\"")
          .append(",\"itemCount\":").append(order.getItemCount())
          .append(",\"items\":[");

        List<OrderItem> items = order.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(orderItemToJson(items.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Builds JSON array from a list of Orders. */
    static String orderListToJson(List<Order> orders) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(orderToJson(orders.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // API Handlers
    // -----------------------------------------------------------------------

    /** GET /api/menu — returns full menu catalog as JSON array */
    static class MenuHandler implements HttpHandler {
        private final RestaurantService service;
        MenuHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            MenuItem[] menu = service.getMenu();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < menu.length; i++) {
                if (i > 0) sb.append(",");
                MenuItem m = menu[i];
                sb.append("{\"id\":").append(m.getId())
                  .append(",\"name\":\"").append(escapeJson(m.getName())).append("\"")
                  .append(",\"category\":\"").append(escapeJson(m.getCategory())).append("\"")
                  .append(",\"description\":\"").append(escapeJson(m.getDescription())).append("\"")
                  .append(",\"price\":").append(m.getPrice())
                  .append(",\"imageUrl\":\"").append(escapeJson(m.getImageUrl())).append("\"")
                  .append("}");
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        }
    }

    /** GET /api/cart — returns current cart state */
    static class CartHandler implements HttpHandler {
        private final RestaurantService service;
        CartHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            sendJson(exchange, 200, cartToJson(service));
        }
    }

    /** POST /api/cart/items/add — adds one unit of itemId to cart */
    static class AddItemHandler implements HttpHandler {
        private final RestaurantService service;
        AddItemHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            try {
                Map<String, String> fields = parseFormBody(exchange);
                int itemId = Integer.parseInt(fields.getOrDefault("itemId", "0"));
                service.addItem(itemId);
                sendJson(exchange, 200, cartToJson(service));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("Invalid itemId"));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 404, jsonError(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
            }
        }
    }

    /** POST /api/cart/items/decrease — decreases qty of itemId by one */
    static class DecreaseItemHandler implements HttpHandler {
        private final RestaurantService service;
        DecreaseItemHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            try {
                Map<String, String> fields = parseFormBody(exchange);
                int itemId = Integer.parseInt(fields.getOrDefault("itemId", "0"));
                service.decreaseItem(itemId);
                sendJson(exchange, 200, cartToJson(service));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("Invalid itemId"));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, jsonError(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
            }
        }
    }

    /** POST /api/cart/items/remove — removes the cart line for itemId entirely */
    static class RemoveItemHandler implements HttpHandler {
        private final RestaurantService service;
        RemoveItemHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            try {
                Map<String, String> fields = parseFormBody(exchange);
                int itemId = Integer.parseInt(fields.getOrDefault("itemId", "0"));
                service.removeItem(itemId);
                sendJson(exchange, 200, cartToJson(service));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("Invalid itemId"));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, jsonError(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
            }
        }
    }

    /** POST /api/cart/undo — pops the last CartAction and restores the prior quantity */
    static class CartUndoHandler implements HttpHandler {
        private final RestaurantService service;
        CartUndoHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            boolean undone = service.undoLastAction();
            if (!undone) {
                sendJson(exchange, 400, jsonError("Nothing to undo"));
                return;
            }
            sendJson(exchange, 200, cartToJson(service));
        }
    }

    /** POST /api/cart/clear — clears cart and undo history */
    static class CartClearHandler implements HttpHandler {
        private final RestaurantService service;
        CartClearHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            service.clearCart();
            sendJson(exchange, 200, cartToJson(service));
        }
    }

    /**
     * /api/orders & /api/history:
     * - GET: returns permanent session order history
     * - POST: validates cart, places new order, and enqueues to kitchen queue (201)
     */
    static class OrdersHandler implements HttpHandler {
        private final RestaurantService service;
        OrdersHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                List<Order> history = service.getOrderHistory();
                sendJson(exchange, 200, orderListToJson(history));
            } else if ("POST".equals(method)) {
                try {
                    Order order = service.placeOrder();
                    sendJson(exchange, 201, orderToJson(order));
                } catch (IllegalStateException e) {
                    sendJson(exchange, 400, jsonError(e.getMessage()));
                } catch (Exception e) {
                    sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
                }
            } else {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
            }
        }
    }

    /**
     * GET /api/kitchen:
     * Returns FIFO pending queue snapshot, front/rear order IDs, and active order.
     */
    static class KitchenHandler implements HttpHandler {
        private final RestaurantService service;
        KitchenHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            List<Order> queueSnapshot = service.getKitchenQueueSnapshot();
            Integer frontId = service.getFrontOrderId();
            Integer rearId = service.getRearOrderId();
            Order active = service.getActiveOrder();

            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"queue\":").append(orderListToJson(queueSnapshot))
              .append(",\"queueCount\":").append(queueSnapshot.size())
              .append(",\"frontOrderId\":").append(frontId != null ? frontId : "null")
              .append(",\"rearOrderId\":").append(rearId != null ? rearId : "null")
              .append(",\"activeOrder\":").append(orderToJson(active))
              .append("}");

            sendJson(exchange, 200, sb.toString());
        }
    }

    /**
     * POST /api/kitchen/start-next:
     * Dequeues oldest order from kitchenQueue in O(1) time and sets to PREPARING as active order.
     */
    static class KitchenStartNextHandler implements HttpHandler {
        private final RestaurantService service;
        KitchenStartNextHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            try {
                Order started = service.startNextOrder();
                sendJson(exchange, 200, orderToJson(started));
            } catch (IllegalStateException e) {
                sendJson(exchange, 400, jsonError(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
            }
        }
    }

    /**
     * POST /api/kitchen/mark-ready:
     * Requires active order in PREPARING status; transitions to READY.
     */
    static class KitchenMarkReadyHandler implements HttpHandler {
        private final RestaurantService service;
        KitchenMarkReadyHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            try {
                Order ready = service.markActiveOrderReady();
                sendJson(exchange, 200, orderToJson(ready));
            } catch (IllegalStateException e) {
                sendJson(exchange, 400, jsonError(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
            }
        }
    }

    /**
     * POST /api/kitchen/complete:
     * Requires active order in READY status; transitions to COMPLETED and clears active station.
     */
    static class KitchenCompleteHandler implements HttpHandler {
        private final RestaurantService service;
        KitchenCompleteHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            try {
                Order completed = service.completeActiveOrder();
                sendJson(exchange, 200, orderToJson(completed));
            } catch (IllegalStateException e) {
                sendJson(exchange, 400, jsonError(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
            }
        }
    }

    /**
     * GET /api/dashboard:
     * Returns live operational metrics and recent session orders from order history.
     */
    static class DashboardHandler implements HttpHandler {
        private final RestaurantService service;
        DashboardHandler(RestaurantService service) { this.service = service; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, jsonError("Method Not Allowed"));
                return;
            }
            List<Order> recent = service.getRecentOrders(10);
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"totalOrders\":").append(service.getTotalOrdersPlaced())
              .append(",\"pendingCount\":").append(service.getPendingOrderCount())
              .append(",\"preparingCount\":").append(service.getPreparingOrderCount())
              .append(",\"readyCount\":").append(service.getReadyOrderCount())
              .append(",\"completedCount\":").append(service.getCompletedOrderCount())
              .append(",\"completedSales\":").append(service.getCompletedSales())
              .append(",\"recentOrders\":").append(orderListToJson(recent))
              .append("}");

            sendJson(exchange, 200, sb.toString());
        }
    }

    // -----------------------------------------------------------------------
    // Static File Server
    // -----------------------------------------------------------------------

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {}
    }

    static class StaticFileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                    && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendPlain(exchange, 405, "Method Not Allowed");
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath == null || requestPath.isEmpty() || requestPath.equals("/")) {
                requestPath = "/index.html";
            }
            while (requestPath.startsWith("/")) {
                requestPath = requestPath.substring(1);
            }

            Path requestedFile = FRONTEND_DIR.resolve(requestPath).normalize();
            if (!requestedFile.startsWith(FRONTEND_DIR)) {
                sendPlain(exchange, 403, "Access Denied");
                return;
            }
            if (Files.isDirectory(requestedFile)) {
                requestedFile = requestedFile.resolve("index.html").normalize();
            }

            File file = requestedFile.toFile();
            if (!file.exists() || !file.isFile()) {
                sendPlain(exchange, 404, "Not Found: " + requestPath);
                return;
            }

            String mimeType = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

            boolean isGet = "GET".equalsIgnoreCase(exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, isGet ? file.length() : -1);
            if (isGet) {
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fis = new FileInputStream(file)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
                }
            } else {
                exchange.getResponseBody().close();
            }
        }

        private void sendPlain(HttpExchange exchange, int code, String text) throws IOException {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }

        private String getMimeType(String filename) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0 && dot < filename.length() - 1) {
                String mime = MIME_TYPES.get(filename.substring(dot + 1).toLowerCase());
                if (mime != null) return mime;
            }
            return "application/octet-stream";
        }
    }
}
