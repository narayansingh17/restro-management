# Olive & Ember — Restaurant Order Management System
## Master Implementation Plan (`plan.md`)

---

### Project Overview
**Olive & Ember** is a student-level restaurant order management application featuring a **responsive web frontend (HTML5/CSS3/Vanilla JS)** served by a **lightweight Java 17 backend server (`com.sun.net.httpserver.HttpServer`)**. It is designed as an academic Data Structures and Algorithms (DSA) mini-project showcasing two foundational data structures implemented from scratch in pure Java:
1. **Custom Singly Linked-List Queue (`LinkedOrderQueue`)**: Implements FIFO (First-In, First-Out) order dispatching for kitchen order processing.
2. **Custom Singly Linked-List Stack (`LinkedCartActionStack`)**: Implements LIFO (Last-In, First-Out) tracking for a single-level Undo feature in the order cart.

The system is entirely in-memory, dependency-free (standard Java JDK only, no external frameworks or libraries), and styled with a modern, calm restaurant POS visual design suitable for college project viva demonstrations.

---

### Technology & Architectural Guidelines

- **Frontend Presentation**:
  - Semantic HTML5 for markup and structure.
  - Modern CSS3 with CSS Custom Properties (variables), Flexbox, CSS Grid, and responsive breakpoints (1440px, 1024px, 768px, 390px, 320px).
  - Vanilla JavaScript for view routing, responsive drawer toggles, live category filtering, and honest UI feedback toasts.
  - Zero external frontend dependencies: No React, Next.js, Tailwind, Bootstrap, Node.js, or npm build tools.
  - Self-contained system font stack and inline SVG icons (no external CDNs or external web fonts).
- **Backend & Serving**:
  - Java 17 standard runtime.
  - Java's built-in `com.sun.net.httpserver.HttpServer` bound to `localhost:8080`.
  - Safe static file serving with path traversal prevention and appropriate MIME types.
  - Zero third-party Java frameworks (no Spring Boot, Tomcat, or third-party web servers).
- **Data Persistence**:
  - In-memory storage throughout runtime lifecycle (no database, no disk serialization, no networking).
- **DSA Core**:
  - Pure Java 17 data structures (`LinkedOrderQueue`, `LinkedCartActionStack`) powering backend logic.
  - Frontend communicates with Java endpoints in subsequent phases.
- **Styling & Aesthetics**:
  - Warm Ivory Canvas: `#F7F5F0`
  - Deep Forest Charcoal Sidebar: `#202820`
  - Restrained Terracotta Accent: `#B85D3F`
  - Soft Sage Card/Surface: `#E7EBE2`
  - Crisp high-contrast typography using clean system sans-serif fonts.
  - Spacing scale: 4px, 8px, 12px, 16px, 24px, 32px.
  - Clean card borders, no emoji dependency, subtle dividers.

---

### Key Data Structures & Complexity Guarantees (Java Backend)

| Structure | Custom Class | Underlying Concept | Key Operations | Time Complexity | Academic Role |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Kitchen Queue** | `LinkedOrderQueue` | Singly Linked List (`front`, `rear`, `size`) | `enqueue()`, `dequeue()`, `peek()`, `isEmpty()`, `size()` | All operations $O(1)$ | FIFO management of incoming food orders for kitchen dispatch |
| **Cart Undo Stack** | `LinkedCartActionStack` | Singly Linked List (`top`, `size`) | `push()`, `pop()`, `peek()`, `isEmpty()`, `size()`, `clear()` | All operations $O(1)$ | LIFO tracking of quantity adjustments to support "Undo Last Change" |
| **Menu Catalog** | Fixed Array | Contiguous Memory Array | Index traversal / lookup | $O(1)$ by index / $O(n)$ search | Fixed catalog of restaurant items |
| **Order History** | `ArrayList<Order>` | Dynamic Array | Append, iterate | $O(1)$ amortized append, $O(n)$ traversal | Complete session log for KPI calculation & history table |

---

### Phased Roadmap Summary

- **Phase 1**: Responsive HTML5/CSS3/JS web frontend and Java 17 static file server (`HttpServer` on `localhost:8080`).
- **Phase 2**: Core domain models, custom `LinkedOrderQueue`, custom `LinkedCartActionStack`, and standalone CLI DSA verification runner.
- **Phase 3**: Java-backed menu catalog, active cart state, dynamic billing, stack-powered Undo endpoint, and order placement workflow.
- **Phase 4**: Java-backed kitchen FIFO queue workflow, order status transitions (`PREPARING` $\rightarrow$ `READY` $\rightarrow$ `COMPLETED`), session KPI metrics, and historical order audit log.
- **Phase 5**: Full end-to-end integration testing, responsive UI polish, comprehensive `README.md`, and viva preparation documentation.

---

## Detailed Phase Breakdown

---

### Phase 1: Responsive Web Frontend and Java Static-File Server

#### Objective and Scope
Establish the project foundation with a lightweight Java 17 HTTP server and an editorial, responsive restaurant POS web frontend. Replace the previous desktop Swing UI with HTML5, CSS3, and Vanilla JavaScript, served locally at `http://localhost:8080`. Implement single-page navigation across Overview, New Order, Kitchen, and Order History, with responsive adaptations for desktop, tablet, and mobile viewports.

#### Prerequisites
- Java 17 JDK installed and verified via `javac -version` / `java -version`.

#### Exact Files to Create / Modify
- `src/com/oliveember/Main.java`: Built-in `HttpServer` serving `frontend/` on `localhost:8080` safely with path traversal protection.
- `frontend/index.html`: Semantic HTML5 markup for the sidebar, mobile topbar, header, and 4 primary view containers.
- `frontend/styles.css`: CSS3 styling system with design tokens, responsive breakpoints, flex/grid layouts, cards, and animations.
- `frontend/app.js`: Vanilla JS managing navigation, mobile drawer toggle, category filtering, search, and action feedback toasts.

#### Ordered Implementation Steps
1. Create `frontend/` directory structure.
2. Implement `src/com/oliveember/Main.java` using `com.sun.net.httpserver.HttpServer` bound to `127.0.0.1:8080`.
   - Add security verification ensuring canonical paths start with `frontend/`.
   - Add correct MIME type mappings (`.html`, `.css`, `.js`, `.svg`, `.png`, `.json`, `.ico`).
   - Default root `/` request to `index.html`.
3. Implement `frontend/index.html`:
   - Mobile topbar with accessible hamburger toggle button.
   - Left sidebar with "Olive & Ember" identity and navigation buttons (Overview, New Order, Kitchen, Order History).
   - Top contextual app header with active section title, breadcrumb, and status badge.
   - View 1 (Overview): KPI metric cards ($2 \times 3$ grid), recent activity list, operational pipeline guide.
   - View 2 (New Order): Search bar, category chips, responsive menu item grid, and dedicated right-hand cart panel.
   - View 3 (Kitchen): FIFO queue visual inspector with explicit `[FRONT - NEXT DISPATCH]` and `[REAR - LAST ADDED]` flags, active station card with progression steps.
   - View 4 (Order History): Styled audit log table with status badges and search filter.
4. Implement `frontend/styles.css`:
   - Design tokens: Warm Ivory (`#F7F5F0`), Charcoal (`#202820`), Terracotta (`#B85D3F`), Soft Sage (`#E7EBE2`).
   - Clean system sans-serif typography, card borders, and subtle shadows.
   - Responsive breakpoints for Desktop (1440px, 1024px), Tablet (768px), and Mobile (390px, 320px).
   - Off-canvas mobile navigation drawer with backdrop overlay.
   - Respect `prefers-reduced-motion`.
5. Implement `frontend/app.js`:
   - Client-side view switching without page reloads.
   - Mobile drawer toggle and ESC key close handler.
   - Interactive category filtering and search for the preview menu catalog.
   - Clear, honest toast notifications explaining that order placement, Queue, and Stack integration occur in Phases 3 and 4.
6. Compile Java server and verify execution on `localhost:8080`.

#### Acceptance Criteria
- Server compiles cleanly and serves static files at `http://localhost:8080/`.
- All CSS, JavaScript, and inline SVG icons load with HTTP 200 and proper MIME types.
- Single-page navigation works seamlessly without page reloads.
- Responsive layout verified across desktop, tablet, and mobile viewports (no horizontal overflow).
- Mobile navigation drawer opens and closes cleanly.
- Action buttons display informative toasts stating their upcoming Java backend connections.
- Path traversal outside `frontend/` is blocked with HTTP 404/403.

#### Manual Test Cases
1. **TC1.1 - Server Startup & Asset Delivery**: Launch server; verify HTTP 200 for `/`, `/styles.css`, and `/app.js`.
2. **TC1.2 - View Switching**: Click "New Order", "Kitchen", "Order History", and "Overview". Verify smooth tab transitions.
3. **TC1.3 - Menu Filtering**: Type in search box and click category chips ("Pizzas", "Mains"). Verify grid updates.
4. **TC1.4 - Mobile Responsiveness**: View at 390px width. Confirm mobile topbar appears, hamburger drawer functions, and layouts stack with zero horizontal scroll.
5. **TC1.5 - Honest Feedback**: Click "+ Add", "Undo Change", or "Start Next Order". Confirm informative toast appears without fabricated state.

#### Explicitly Excluded Work
- No fake or mocked order-processing logic.
- Java DSA Queue and Stack are reserved for subsequent phases.

#### Phase 1 Verification Checklist
- [x] Lightweight Java 17 `HttpServer` implemented on `localhost:8080`
- [x] Path traversal security and MIME mapping verified
- [x] `frontend/index.html` created with semantic structure and all 4 views
- [x] `frontend/styles.css` implemented with brand tokens and full responsive breakpoints
- [x] `frontend/app.js` implemented with SPA routing, search, drawer, and toasts
- [x] Tested and verified across desktop and mobile viewports

---

### Phase 2: Data Models, Custom Linked-List Queue, Custom Linked-List Stack & DSA Verification

#### Objective and Scope
Implement the pure domain models and the two required core custom data structures from scratch in Java using singly linked lists. Create an automated, dependency-free command-line verification runner (`DSAVerifier.java`) to thoroughly test boundary cases, empty states, and $O(1)$ operations before integrating them with the server.

#### Prerequisites
- Phase 1 verified and complete.

#### Exact Files to Create / Modify
- `src/com/oliveember/model/MenuItem.java`: Immutable menu item descriptor (id, name, category, description, price).
- `src/com/oliveember/model/OrderItem.java`: Order line item pairing a `MenuItem` with an integer quantity and line total.
- `src/com/oliveember/model/OrderStatus.java`: Enum (`PENDING`, `PREPARING`, `READY`, `COMPLETED`).
- `src/com/oliveember/model/Order.java`: Order entity (orderId, list of OrderItems, totalAmount, formatted timestamp, current status).
- `src/com/oliveember/model/CartAction.java`: Undo action recording affected `menuItemId` and `previousQuantity`.
- `src/com/oliveember/dsa/LinkedOrderQueue.java`: Custom singly linked-list Queue for kitchen FIFO processing.
- `src/com/oliveember/dsa/LinkedCartActionStack.java`: Custom singly linked-list Stack for cart Undo operations.
- `src/com/oliveember/dsa/DSAVerifier.java`: Standalone main class testing queue and stack corner cases.

#### Ordered Implementation Steps
1. Create domain models (`MenuItem`, `OrderItem`, `OrderStatus`, `Order`, `CartAction`).
2. Implement `LinkedOrderQueue.java`:
   - Singly linked list with `front`, `rear`, `size`.
   - `enqueue(Order order)`: Inserts at `rear`. $O(1)$.
   - `dequeue()`: Removes from `front`. Handles single-element edge case by clearing both `front` and `rear`. Returns `null` on empty. $O(1)$.
   - `peek()`, `isEmpty()`, `size()`, `toList()`.
3. Implement `LinkedCartActionStack.java`:
   - Singly linked list with `top`, `size`.
   - `push(CartAction action)`: Prepends to `top`. $O(1)$.
   - `pop()`: Pops from `top`. Returns `null` on empty. $O(1)$.
   - `peek()`, `isEmpty()`, `size()`, `clear()`.
4. Implement `DSAVerifier.java`:
   - Automated test suite validating FIFO order, LIFO undo order, boundary states, and single-element removal.
5. Compile and run `DSAVerifier`.

#### Acceptance Criteria
- Zero dependencies on `java.util.Queue`, `java.util.LinkedList`, `java.util.Stack`, or `java.util.Deque`.
- All queue and stack operations run strictly in $O(1)$ time.
- `DSAVerifier` runs completely and prints 100% passing assertions.

#### Phase 2 Verification Checklist
- [x] Domain models implemented
- [x] `LinkedOrderQueue` implemented from scratch
- [x] `LinkedCartActionStack` implemented from scratch
- [x] `DSAVerifier.java` implemented with automated test scenarios
- [x] Verified all test cases pass cleanly via command line execution

---

### Phase 3: Java-Backed Menu/Cart Integration, Billing, Stack-Powered Undo & Order Placement

#### Objective and Scope
Implement the in-memory `RestaurantService` in Java, expose RESTful HTTP endpoints for menu retrieval, cart manipulation, bill computation, stack-powered Undo, and order placement. Connect the web frontend to these endpoints so the New Order page operates against live Java state.

#### Prerequisites
- Phase 1 and Phase 2 verified and complete.

#### Exact Files to Create / Modify
- `src/com/oliveember/service/RestaurantService.java`: Central application coordinator managing menu array, active cart, `LinkedCartActionStack`, `LinkedOrderQueue`, and `orderHistory`.
- `src/com/oliveember/Main.java`: Add API contexts (`/api/menu`, `/api/cart`, `/api/cart/add`, `/api/cart/update`, `/api/cart/undo`, `/api/cart/clear`, `/api/order/place`).
- `frontend/app.js`: Wire New Order UI to call Java API endpoints and update the cart, bill, and undo state dynamically.

#### Ordered Implementation Steps
1. Implement `RestaurantService.java` with fixed `MenuItem[]` catalog and active cart methods.
2. Wire `LinkedCartActionStack`:
   - When quantity is adjusted, push `CartAction(menuItemId, previousQty)`.
   - On `/api/cart/undo`, pop `CartAction` and restore previous quantity.
   - On order placement or cart clear, clear the undo stack.
3. Expose lightweight JSON endpoints in `Main.java` using standard Java.
4. Update `frontend/app.js` to fetch `/api/menu` and manage cart operations through Java backend calls.

#### Acceptance Criteria
- Fixed menu catalog served from Java backend array.
- Cart quantity adjustments push to Java `LinkedCartActionStack`.
- "Undo Change" restores previous quantity via Java stack $O(1)$ pop.
- Placing order enqueues into Java `LinkedOrderQueue` ($O(1)$) and clears cart and stack.

#### Phase 3 Verification Checklist
- [x] `RestaurantService` created with catalog, cart, and stack integration
- [x] API endpoints for cart and undo wired in Java `HttpServer`
- [x] Web frontend dynamically renders menu and synchronizes cart with backend
- [x] Stack-backed Undo feature verified live in browser
- [x] Order placement generates sequential ID and enqueues order in Java

---

### Phase 4: Java-Backed Kitchen Workflow, Queue Integration, Dashboard & History

#### Objective and Scope
Connect the Kitchen, Overview, and History screens to the Java backend. The Kitchen page will display real tickets from `LinkedOrderQueue` (Front-to-Rear), support dequeuing to an active station slot, and manage the `PREPARING` $\rightarrow$ `READY` $\rightarrow$ `COMPLETED` state machine. Overview and History will reflect live session data.

#### Prerequisites
- Phases 1, 2, and 3 verified and complete.

#### Exact Files to Create / Modify
- `src/com/oliveember/service/RestaurantService.java`: Add kitchen queue dispatch and active order lifecycle methods.
- `src/com/oliveember/Main.java`: Add API contexts (`/api/kitchen/queue`, `/api/kitchen/start`, `/api/kitchen/ready`, `/api/kitchen/complete`, `/api/dashboard`, `/api/history`).
- `frontend/app.js`: Update Kitchen, Overview, and History views to poll or refresh live data from Java endpoints.

#### Ordered Implementation Steps
1. Enhance `RestaurantService.java`:
   - `startNextOrder()`: Dequeues from `LinkedOrderQueue` ($O(1)$) and sets active order to `PREPARING`.
   - Enforce single active preparation order rule.
   - Transition to `READY` and `COMPLETED`.
   - Aggregate live KPI metrics (completed revenue counts completed orders only).
2. Register API endpoints in `Main.java`.
3. Update `frontend/app.js` to render live kitchen queue, front/rear badges, active station controls, KPI cards, and history table.

#### Acceptance Criteria
- Kitchen displays orders strictly in FIFO order directly from `LinkedOrderQueue`.
- Front and Rear pointers are clearly indicated.
- "Start Next Order" dequeues front ticket in $O(1)$ time.
- Single-order constraint enforced in active station.
- Completed sales only counts completed orders.
- Order history displays permanent audit log.

#### Phase 4 Verification Checklist
- [x] Kitchen workflow methods integrated in `RestaurantService`
- [x] Kitchen API endpoints registered in Java server
- [x] Web UI displays live pending queue with FRONT and REAR badges
- [x] Active station state machine (`PREPARING` $\rightarrow$ `READY` $\rightarrow$ `COMPLETED`) verified
- [x] Overview dashboard KPIs reflect real backend data
- [x] Order history table reflects complete session records

---

### Phase 5: Integration Testing, Responsive Polish, README & Viva Preparation

#### Objective and Scope
Conduct comprehensive end-to-end testing across all viewports and browser environments, polish edge cases (empty states, button disabled states, input sanitization), author the comprehensive `README.md`, and compile 10+ viva questions with concise model answers.

#### Prerequisites
- Phases 1 through 4 verified and operational.

#### Exact Files to Create / Modify
- `README.md`: System overview, exact execution commands, project architecture, DSA deep-dive, time complexity table, and viva Q&A.
- Minor fixes across frontend and backend files if required.

#### Acceptance Criteria
- Zero runtime errors or broken assets.
- Complete responsive verification at 1440px, 1024px, 768px, 390px, 320px.
- Both custom data structures completely free of Java collection replacements.
- `README.md` complete with runnable commands and comprehensive academic viva preparation.

#### Phase 5 Verification Checklist
- [ ] Comprehensive end-to-end testing executed
- [ ] Responsive design verified across all viewports
- [ ] `README.md` created with complete run instructions
- [ ] Detailed DSA viva question-and-answer guide included
- [ ] Final project ready for college demonstration

---

### Strict Execution Rules
- No source code implementation will be done before explicit phase triggers.
- The plan will remain the single source of truth and phase checklists will be updated incrementally upon verified completion.
