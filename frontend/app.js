/**
 * Olive & Ember — Restaurant Order Management System
 * Frontend Application Script (Phase 4)
 *
 * Full multi-view integration with Java 17 backend:
 * - Overview: live dashboard metrics & recent session orders via /api/dashboard
 * - New Order: dynamic menu, server-managed cart, stack-backed Undo (/api/cart, /api/orders)
 * - Kitchen: FIFO queue visualization via LinkedOrderQueue, single-station workflow (/api/kitchen)
 * - Order History: permanent session audit log with search filter (/api/orders)
 */

document.addEventListener("DOMContentLoaded", () => {
  initNavigation();
  initMobileDrawer();
  initHistoryFilter();
  initNewOrderPage();
  initKitchenActions();

  // Initial data load for all views
  loadDashboard();
  loadKitchen();
  loadHistory();
});

/* ==========================================================================
   Navigation
   ========================================================================== */

const VIEW_METADATA = {
  "overview": {
    title: "Restaurant Overview",
    subtitle: "Real-time operational snapshot of active orders and kitchen queue status."
  },
  "new-order": {
    title: "New Order Entry",
    subtitle: "Select artisanal items from the catalog, configure portions, and dispatch tickets to the kitchen."
  },
  "kitchen": {
    title: "Kitchen Station Workflow",
    subtitle: "Live kitchen ticket management and order preparation workflow."
  },
  "history": {
    title: "Session Order History",
    subtitle: "Audit log of all orders placed during this session."
  }
};

function initNavigation() {
  const navButtons = document.querySelectorAll(".nav-item");
  const viewSections = document.querySelectorAll(".view-section");
  const pageTitle = document.getElementById("page-title");
  const pageSubtitle = document.getElementById("page-subtitle");

  navButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetView = btn.getAttribute("data-view");
      if (!targetView) return;

      navButtons.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");

      viewSections.forEach(section => {
        section.classList.remove("active");
        if (section.id === `view-${targetView}`) section.classList.add("active");
      });

      if (VIEW_METADATA[targetView]) {
        pageTitle.textContent = VIEW_METADATA[targetView].title;
        pageSubtitle.textContent = VIEW_METADATA[targetView].subtitle;
      }

      closeMobileDrawer();
      const mainContent = document.querySelector(".main-content");
      if (mainContent) mainContent.scrollTop = 0;

      // Refresh view-specific live data upon navigation
      if (targetView === "overview") loadDashboard();
      if (targetView === "kitchen") loadKitchen();
      if (targetView === "history") loadHistory();
      if (targetView === "new-order") loadCart();
    });
  });
}

/* ==========================================================================
   Mobile Drawer
   ========================================================================== */

function initMobileDrawer() {
  const menuBtn = document.getElementById("mobile-menu-btn");
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("sidebar-overlay");
  if (!menuBtn || !sidebar || !overlay) return;

  menuBtn.addEventListener("click", () => {
    sidebar.classList.toggle("open");
    overlay.classList.toggle("active");
  });
  overlay.addEventListener("click", closeMobileDrawer);
  document.addEventListener("keydown", e => {
    if (e.key === "Escape" && sidebar.classList.contains("open")) closeMobileDrawer();
  });
}

function closeMobileDrawer() {
  const sidebar = document.getElementById("sidebar");
  const overlay = document.getElementById("sidebar-overlay");
  if (sidebar) sidebar.classList.remove("open");
  if (overlay) overlay.classList.remove("active");
}

/* ==========================================================================
   Toast Notifications
   ========================================================================== */

function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const iconColor = type === "error" ? "#e53e3e" : type === "success" ? "#38A169" : "#B85D3F";
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.innerHTML = `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="${iconColor}" stroke-width="2">
      <circle cx="12" cy="12" r="10"></circle>
      <line x1="12" y1="16" x2="12" y2="12"></line>
      <line x1="12" y1="8" x2="12.01" y2="8"></line>
    </svg>
    <span>${escapeHtml(message)}</span>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.transition = "all 0.2s ease";
    toast.style.opacity = "0";
    toast.style.transform = "translateY(8px)";
    setTimeout(() => { if (toast.parentElement) toast.remove(); }, 200);
  }, 3500);
}

/* ==========================================================================
   API Helpers
   ========================================================================== */

async function apiFetch(url, options = {}) {
  const res = await fetch(url, options);
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || `HTTP ${res.status}`);
  }
  return data;
}

function postForm(url, params = {}) {
  const body = new URLSearchParams(params).toString();
  return apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });
}

function postJson(url) {
  return apiFetch(url, { method: "POST" });
}

function getStatusBadge(status) {
  switch (status) {
    case "PENDING":
      return `<span class="badge badge-amber">PENDING</span>`;
    case "PREPARING":
      return `<span class="badge badge-blue">PREPARING</span>`;
    case "READY":
      return `<span class="badge" style="background:#F8EBE4;color:#B85D3F;border:1px solid #F5C6B7;">READY</span>`;
    case "COMPLETED":
      return `<span class="badge badge-green">COMPLETED</span>`;
    default:
      return `<span class="badge badge-muted">${escapeHtml(status)}</span>`;
  }
}

/* ==========================================================================
   PAGE A: OVERVIEW (DASHBOARD)
   ========================================================================== */

async function loadDashboard() {
  try {
    const data = await apiFetch("/api/dashboard");
    renderDashboard(data);
  } catch (err) {
    console.error("Dashboard fetch error:", err);
  }
}

function renderDashboard(data) {
  // Update KPI counters
  const totalEl = document.getElementById("kpi-total-orders");
  const queueEl = document.getElementById("kpi-queue-count");
  const prepEl  = document.getElementById("kpi-prep-count");
  const readyEl = document.getElementById("kpi-ready-count");
  const compEl  = document.getElementById("kpi-completed-orders");
  const salesEl = document.getElementById("kpi-completed-sales");

  if (totalEl) totalEl.textContent = data.totalOrders ?? 0;
  if (queueEl) queueEl.textContent = data.pendingCount ?? 0;
  if (prepEl)  prepEl.textContent = data.preparingCount ?? 0;
  if (readyEl) readyEl.textContent = data.readyCount ?? 0;
  if (compEl)  compEl.textContent = data.completedCount ?? 0;
  if (salesEl) salesEl.textContent = `₹${(data.completedSales ?? 0).toLocaleString("en-IN")}`;

  // Update Recent Session Orders
  const listEl = document.getElementById("overview-activity-list");
  if (!listEl) return;
  listEl.innerHTML = "";

  const orders = data.recentOrders || [];
  if (orders.length === 0) {
    listEl.innerHTML = `
      <div class="station-idle-box" style="margin:var(--space-md) 0;">
        <p>No session orders placed yet.</p>
        <span class="text-sm">Navigate to <strong>New Order</strong> to create and send orders to the kitchen.</span>
      </div>
    `;
    return;
  }

  orders.forEach(order => {
    const itemsSummary = order.items.map(i => `${escapeHtml(i.name)} × ${i.quantity}`).join(", ");
    const itemDiv = document.createElement("div");
    itemDiv.className = "activity-item";
    itemDiv.innerHTML = `
      <div class="activity-left">
        <span class="order-chip">#${order.orderId}</span>
        <div>
          <p class="order-main-text">${itemsSummary}</p>
          <span class="order-sub-text">${escapeHtml(order.createdAt)} • ${order.itemCount} item${order.itemCount !== 1 ? "s" : ""}</span>
        </div>
      </div>
      <div class="activity-right">
        <span class="order-amount">₹${order.totalAmount.toLocaleString("en-IN")}</span>
        ${getStatusBadge(order.status)}
      </div>
    `;
    listEl.appendChild(itemDiv);
  });
}

/* ==========================================================================
   PAGE B: NEW ORDER
   ========================================================================== */

let activeCategory = "all";
let searchTerm = "";

function initNewOrderPage() {
  loadMenu();
  loadCart();
  initCategoryChips();
  initSearchBox();
  initCartActions();
}

const MENU_STOCK_IMAGES = {
  1: "/images/margherita.jpg",
  2: "/images/flatbread.jpg",
  3: "/images/risotto.jpg",
  4: "/images/ravioli.jpg",
  5: "/images/focaccia.jpg",
  6: "/images/burrata.jpg",
  7: "/images/tiramisu.jpg",
  8: "/images/cake.jpg",
  9: "/images/hibiscus.jpg",
  10: "/images/coldbrew.jpg"
};

async function loadMenu() {
  const grid = document.getElementById("menu-items-grid");
  if (!grid) return;

  grid.innerHTML = `<div class="menu-loading-msg">Loading artisanal menu…</div>`;
  try {
    const items = await apiFetch("/api/menu");
    renderMenuGrid(items);
  } catch (err) {
    grid.innerHTML = `<div class="menu-loading-msg text-terracotta">Could not load menu: ${escapeHtml(err.message)}</div>`;
    showToast("Failed to load menu from server.", "error");
  }
}

function renderMenuGrid(items) {
  const grid = document.getElementById("menu-items-grid");
  if (!grid) return;
  grid.innerHTML = "";

  const normalizeCategory = cat => cat.toLowerCase().replace(/\s+/g, "-");

  items.forEach(item => {
    const catSlug = normalizeCategory(item.category);
    const card = document.createElement("div");
    card.className = "card menu-card";
    card.dataset.category = catSlug;
    card.dataset.id = item.id;
    const imgUrl = item.imageUrl || MENU_STOCK_IMAGES[item.id] || "/images/margherita.jpg";
    card.innerHTML = `
      <div class="menu-card-img-wrap">
        <img src="${escapeHtml(imgUrl)}" alt="${escapeHtml(item.name)}" class="menu-card-img" loading="lazy" onerror="this.onerror=null;this.src='/images/margherita.jpg';">
        <span class="menu-card-category-badge badge badge-sage">${escapeHtml(item.category.toUpperCase())}</span>
      </div>
      <div class="menu-card-body">
        <h4 class="menu-card-title">${escapeHtml(item.name)}</h4>
        <p class="menu-card-desc">${escapeHtml(item.description)}</p>
      </div>
      <div class="menu-card-footer">
        <span class="menu-card-price">₹${item.price}</span>
        <button class="btn btn-primary btn-sm" data-add-id="${item.id}" aria-label="Add ${escapeHtml(item.name)} to cart">+ Add</button>
      </div>
    `;
    card.querySelector(`[data-add-id]`).addEventListener("click", () => handleAddToCart(item.id));
    grid.appendChild(card);
  });

  applyMenuFilter();
}

function initCategoryChips() {
  document.querySelectorAll(".cat-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      document.querySelectorAll(".cat-chip").forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      activeCategory = chip.getAttribute("data-category") || "all";
      applyMenuFilter();
    });
  });
}

function initSearchBox() {
  const input = document.getElementById("menu-search-input");
  if (!input) return;
  input.addEventListener("input", e => {
    searchTerm = e.target.value.trim().toLowerCase();
    applyMenuFilter();
  });
}

function applyMenuFilter() {
  document.querySelectorAll(".menu-card").forEach(card => {
    const cat = card.dataset.category || "";
    const title = card.querySelector(".menu-card-title")?.textContent.toLowerCase() || "";
    const desc = card.querySelector(".menu-card-desc")?.textContent.toLowerCase() || "";
    const matchesCat = activeCategory === "all" || cat === activeCategory;
    const matchesSearch = !searchTerm || title.includes(searchTerm) || desc.includes(searchTerm);
    card.style.display = (matchesCat && matchesSearch) ? "flex" : "none";
  });
}

async function loadCart() {
  try {
    const cart = await apiFetch("/api/cart");
    renderCart(cart);
  } catch (err) {
    showToast("Could not load cart: " + err.message, "error");
  }
}

function renderCart(cart) {
  renderCartItems(cart.items);
  renderBillSummary(cart.total);
  updateUndoButton(cart.undoAvailable);
}

function renderCartItems(items) {
  const listEl = document.getElementById("cart-items-list");
  const emptyEl = document.getElementById("cart-empty-state");
  const cartBadge = document.getElementById("cart-badge");
  if (!listEl) return;

  listEl.innerHTML = "";
  const totalQty = items.reduce((s, i) => s + i.quantity, 0);
  if (cartBadge) cartBadge.textContent = totalQty > 0 ? `${totalQty} Item${totalQty !== 1 ? "s" : ""}` : "Empty";
  if (emptyEl) emptyEl.style.display = items.length === 0 ? "flex" : "none";
  listEl.style.display = items.length === 0 ? "none" : "flex";

  items.forEach(item => {
    const row = document.createElement("div");
    row.className = "cart-item";
    const imgUrl = MENU_STOCK_IMAGES[item.menuItemId] || "/images/margherita.jpg";
    row.innerHTML = `
      <div class="cart-item-left">
        <img src="${escapeHtml(imgUrl)}" class="cart-item-thumb" alt="${escapeHtml(item.name)}" onerror="this.style.display='none'">
        <div class="cart-item-info">
          <p class="cart-item-name">${escapeHtml(item.name)}</p>
          <span class="cart-item-unit">₹${item.unitPrice} each</span>
        </div>
      </div>
      <div class="cart-item-controls">
        <button class="qty-btn" data-dec-id="${item.menuItemId}" aria-label="Decrease ${escapeHtml(item.name)}">−</button>
        <span class="cart-qty">${item.quantity}</span>
        <button class="qty-btn" data-inc-id="${item.menuItemId}" aria-label="Increase ${escapeHtml(item.name)}">+</button>
      </div>
      <span class="cart-item-total">₹${item.subtotal}</span>
    `;
    row.querySelector(`[data-dec-id]`).addEventListener("click", () => handleDecrease(item.menuItemId));
    row.querySelector(`[data-inc-id]`).addEventListener("click", () => handleAddToCart(item.menuItemId));
    listEl.appendChild(row);
  });
}

function renderBillSummary(total) {
  const subtotalEl = document.getElementById("bill-subtotal");
  const totalEl = document.getElementById("bill-total");
  if (subtotalEl) subtotalEl.textContent = `₹${total}`;
  if (totalEl) totalEl.textContent = `₹${total}`;
}

function updateUndoButton(available) {
  const undoBtn = document.getElementById("undo-btn");
  if (undoBtn) undoBtn.disabled = !available;
}

async function handleAddToCart(itemId) {
  try {
    const cart = await postForm("/api/cart/items/add", { itemId });
    renderCart(cart);
  } catch (err) {
    showToast("Could not add item: " + err.message, "error");
  }
}

async function handleDecrease(itemId) {
  try {
    const cart = await postForm("/api/cart/items/decrease", { itemId });
    renderCart(cart);
  } catch (err) {
    showToast("Could not decrease item: " + err.message, "error");
  }
}

function initCartActions() {
  const undoBtn = document.getElementById("undo-btn");
  const clearBtn = document.getElementById("clear-btn");
  const placeBtn = document.getElementById("place-order-btn");

  if (undoBtn) {
    undoBtn.addEventListener("click", async () => {
      try {
        const cart = await postForm("/api/cart/undo");
        renderCart(cart);
        showToast("Last cart change undone.", "info");
      } catch (err) {
        showToast(err.message || "Nothing to undo.", "info");
      }
    });
  }

  if (clearBtn) {
    clearBtn.addEventListener("click", async () => {
      try {
        const cart = await postForm("/api/cart/clear");
        renderCart(cart);
        showToast("Cart and undo history cleared.", "info");
      } catch (err) {
        showToast("Could not clear cart: " + err.message, "error");
      }
    });
  }

  if (placeBtn) {
    placeBtn.addEventListener("click", handlePlaceOrder);
  }
}

async function handlePlaceOrder() {
  const placeBtn = document.getElementById("place-order-btn");
  if (placeBtn) placeBtn.disabled = true;
  try {
    const order = await postForm("/api/orders");
    showToast(
      `✓ Order #${order.orderId} placed (₹${order.totalAmount}) and sent to kitchen.`,
      "success"
    );
    showOrderConfirmation(order);

    // Synchronize all views with authoritative backend state
    loadCart();
    loadDashboard();
    loadKitchen();
    loadHistory();
  } catch (err) {
    showToast(err.message || "Could not place order.", "error");
  } finally {
    if (placeBtn) placeBtn.disabled = false;
  }
}

function showOrderConfirmation(order) {
  const banner = document.getElementById("order-confirmation-banner");
  if (!banner) return;
  banner.innerHTML = `
    <div class="order-confirm-inner">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#286B34" stroke-width="2.5">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
      <div>
        <strong>Order #${order.orderId} confirmed!</strong>
        Total: ₹${order.totalAmount} · ${order.itemCount} item(s) · Placed at ${escapeHtml(order.createdAt)}
      </div>
      <button class="badge badge-muted" onclick="document.getElementById('order-confirmation-banner').style.display='none'">✕</button>
    </div>
  `;
  banner.style.display = "flex";
}

/* ==========================================================================
   PAGE C: KITCHEN WORKFLOW
   ========================================================================== */

async function loadKitchen() {
  try {
    const data = await apiFetch("/api/kitchen");
    renderKitchen(data);
  } catch (err) {
    console.error("Kitchen fetch error:", err);
  }
}

function renderKitchen(data) {
  const queue = data.queue || [];
  const active = data.activeOrder;
  const frontId = data.frontOrderId;
  const rearId = data.rearOrderId;

  // 1. Queue Header Badge & Pointer Info
  const queueBadge = document.getElementById("kitchen-queue-badge");
  if (queueBadge) queueBadge.textContent = `${queue.length} in Queue`;

  const footerInfo = document.getElementById("queue-footer-info");
  if (footerInfo) {
    footerInfo.innerHTML = "";
  }

  // 2. Pending Tickets List
  const queueList = document.getElementById("kitchen-queue-list");
  if (queueList) {
    queueList.innerHTML = "";
    if (queue.length === 0) {
      queueList.innerHTML = `
        <div class="queue-empty-box">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#A0A89C" stroke-width="1.5">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="9" y1="9" x2="15" y2="15"></line>
            <line x1="15" y1="9" x2="9" y2="15"></line>
          </svg>
          <p>Kitchen queue is currently empty.</p>
          <span class="text-sm">Place an order in <strong>New Order</strong> to send tickets to the kitchen.</span>
        </div>
      `;
    } else {
      queue.forEach((order, index) => {
        const isFront = order.orderId === frontId;
        const isRear = order.orderId === rearId;

        let tagBadge = "";
        if (isFront && isRear) {
          tagBadge = `<span class="badge badge-front">NEXT UP</span>`;
        } else if (isFront) {
          tagBadge = `<span class="badge badge-front">NEXT UP</span>`;
        } else if (isRear) {
          tagBadge = `<span class="badge badge-rear">NEWEST</span>`;
        } else {
          tagBadge = `<span class="badge badge-muted">#${index + 1} In Line</span>`;
        }

        const itemsList = order.items
          .map(i => `<li>${i.quantity} &times; ${escapeHtml(i.name)}</li>`)
          .join("");

        const ticket = document.createElement("div");
        ticket.className = `queue-ticket ${isFront ? "front-ticket" : ""} ${isRear && !isFront ? "rear-ticket" : ""}`;
        ticket.innerHTML = `
          <div class="ticket-header">
            <div class="ticket-id-wrap">
              <span class="ticket-id">Order #${order.orderId}</span>
              ${tagBadge}
            </div>
            <span class="ticket-time">${escapeHtml(order.createdAt)}</span>
          </div>
          <div class="ticket-body">
            <ul class="ticket-items">${itemsList}</ul>
          </div>
          <div class="ticket-footer">
            <span class="ticket-total">Total: ₹${order.totalAmount.toLocaleString("en-IN")}</span>
            <span class="ticket-notes">${order.itemCount} item${order.itemCount !== 1 ? "s" : ""}</span>
          </div>
        `;
        queueList.appendChild(ticket);
      });
    }
  }

  // 3. Active Station Box & Button States
  const stationBadge = document.getElementById("active-station-badge");
  const activeContainer = document.getElementById("active-ticket-container");
  const btnStart = document.getElementById("btn-start-order");
  const btnReady = document.getElementById("btn-mark-ready");
  const btnComplete = document.getElementById("btn-complete-order");

  if (active) {
    const isPrep = active.status === "PREPARING";
    const isReady = active.status === "READY";

    if (stationBadge) {
      stationBadge.textContent = active.status;
      stationBadge.className = isPrep ? "badge badge-blue" : "badge";
      if (isReady) {
        stationBadge.style.background = "#F8EBE4";
        stationBadge.style.color = "#B85D3F";
        stationBadge.style.border = "1px solid #F5C6B7";
      } else {
        stationBadge.style.background = "";
        stationBadge.style.color = "";
        stationBadge.style.border = "";
      }
    }

    const itemsRows = active.items.map(i => `
      <div class="active-item-row">
        <span class="item-name">${i.quantity} &times; ${escapeHtml(i.name)}</span>
        <span class="item-status">${isPrep ? "On Grill / Oven" : "Plated &amp; Ready"}</span>
      </div>
    `).join("");

    const step2Class = isPrep ? "step-current" : "step-done";
    const step2Circle = isPrep ? "2" : "&#10003;";
    const step3Class = isReady ? "step-current" : "";
    const stepLine2Active = !isPrep ? "step-active" : "";

    if (activeContainer) {
      activeContainer.innerHTML = `
        <div class="active-ticket-box">
          <div class="active-ticket-header">
            <div>
              <span class="active-ticket-badge">${isPrep ? "CURRENTLY IN PREPARATION" : "READY FOR SERVICE"}</span>
              <h3 class="active-ticket-title">Order #${active.orderId}</h3>
            </div>
            <span class="active-ticket-time">Placed at ${escapeHtml(active.createdAt)}</span>
          </div>
          <div class="active-ticket-items">${itemsRows}</div>
          <div class="workflow-steps">
            <div class="step-item step-done">
              <span class="step-circle">&#10003;</span>
              <span class="step-text">Pending</span>
            </div>
            <div class="step-line step-active"></div>
            <div class="step-item ${step2Class}">
              <span class="step-circle">${step2Circle}</span>
              <span class="step-text">Preparing</span>
            </div>
            <div class="step-line ${stepLine2Active}"></div>
            <div class="step-item ${step3Class}">
              <span class="step-circle">3</span>
              <span class="step-text">Ready</span>
            </div>
            <div class="step-line"></div>
            <div class="step-item">
              <span class="step-circle">4</span>
              <span class="step-text">Completed</span>
            </div>
          </div>
        </div>
      `;
    }

    // Action control constraints:
    // Only 1 order active at a time: Start is disabled when active order exists
    if (btnStart) btnStart.disabled = true;
    if (btnReady) btnReady.disabled = !isPrep;
    if (btnComplete) btnComplete.disabled = !isReady;

  } else {
    // Station Idle
    if (stationBadge) {
      stationBadge.textContent = "IDLE";
      stationBadge.className = "badge badge-muted";
      stationBadge.style.background = "";
      stationBadge.style.color = "";
      stationBadge.style.border = "";
    }

    if (activeContainer) {
      activeContainer.innerHTML = `
        <div class="station-idle-box">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#A0A89C" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
          <p>Station Idle — No order in preparation.</p>
          <span class="text-sm">Click <strong>Start Next Order</strong> to begin preparation.</span>
        </div>
      `;
    }

    // Can start next order ONLY if pending queue has orders
    if (btnStart) btnStart.disabled = (queue.length === 0);
    if (btnReady) btnReady.disabled = true;
    if (btnComplete) btnComplete.disabled = true;
  }
}

function initKitchenActions() {
  const btnStart = document.getElementById("btn-start-order");
  const btnReady = document.getElementById("btn-mark-ready");
  const btnComplete = document.getElementById("btn-complete-order");

  if (btnStart) {
    btnStart.addEventListener("click", async () => {
      btnStart.disabled = true;
      try {
        const order = await postJson("/api/kitchen/start-next");
        showToast(`✓ Order #${order.orderId} moved to station (PREPARING).`, "success");
        await loadKitchen();
        loadDashboard();
      } catch (err) {
        showToast(err.message, "error");
        await loadKitchen();
      }
    });
  }

  if (btnReady) {
    btnReady.addEventListener("click", async () => {
      btnReady.disabled = true;
      try {
        const order = await postJson("/api/kitchen/mark-ready");
        showToast(`✓ Order #${order.orderId} moved to READY status.`, "success");
        await loadKitchen();
        loadDashboard();
      } catch (err) {
        showToast(err.message, "error");
        await loadKitchen();
      }
    });
  }

  if (btnComplete) {
    btnComplete.addEventListener("click", async () => {
      btnComplete.disabled = true;
      try {
        const order = await postJson("/api/kitchen/complete");
        showToast(`✓ Order #${order.orderId} marked COMPLETED (Station freed, sales credited).`, "success");
        await loadKitchen();
        loadDashboard();
        loadHistory();
      } catch (err) {
        showToast(err.message, "error");
        await loadKitchen();
      }
    });
  }
}

/* ==========================================================================
   PAGE D: ORDER HISTORY
   ========================================================================== */

let allHistoryOrders = [];

async function loadHistory() {
  try {
    const orders = await apiFetch("/api/orders");
    allHistoryOrders = orders || [];
    renderHistoryTable(allHistoryOrders);
  } catch (err) {
    console.error("Order history fetch error:", err);
  }
}

function renderHistoryTable(orders) {
  const tableBody = document.getElementById("history-table-body");
  const countBadge = document.getElementById("history-count-badge");
  const footerText = document.getElementById("history-footer-text");
  if (!tableBody) return;

  tableBody.innerHTML = "";
  if (countBadge) countBadge.textContent = `${orders.length} Order${orders.length !== 1 ? "s" : ""}`;
  if (footerText) footerText.textContent = `Showing ${orders.length} of ${allHistoryOrders.length} session records.`;

  if (orders.length === 0) {
    const row = document.createElement("tr");
    row.innerHTML = `<td colspan="5" style="text-align:center;padding:var(--space-xl);color:var(--text-muted);font-style:italic;">No session orders recorded yet.</td>`;
    tableBody.appendChild(row);
    return;
  }

  orders.forEach(order => {
    const itemsSummary = order.items.map(i => `${escapeHtml(i.name)} (${i.quantity})`).join(", ");
    const row = document.createElement("tr");
    row.innerHTML = `
      <td class="font-bold">#${order.orderId}</td>
      <td>${escapeHtml(order.createdAt)}</td>
      <td>${itemsSummary}</td>
      <td class="font-bold">₹${order.totalAmount.toLocaleString("en-IN")}</td>
      <td>${getStatusBadge(order.status)}</td>
    `;
    tableBody.appendChild(row);
  });
}

function initHistoryFilter() {
  const historySearch = document.getElementById("history-search");
  if (!historySearch) return;

  historySearch.addEventListener("input", e => {
    const term = e.target.value.trim().toLowerCase();
    if (!term) {
      renderHistoryTable(allHistoryOrders);
    } else {
      const filtered = allHistoryOrders.filter(order => {
        const idMatch = (`#${order.orderId}`).toLowerCase().includes(term);
        const statusMatch = order.status.toLowerCase().includes(term);
        const itemMatch = order.items.some(i => i.name.toLowerCase().includes(term));
        return idMatch || statusMatch || itemMatch;
      });
      renderHistoryTable(filtered);
    }
  });
}

/* ==========================================================================
   Utilities
   ========================================================================== */

function escapeHtml(s) {
  if (!s) return "";
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
