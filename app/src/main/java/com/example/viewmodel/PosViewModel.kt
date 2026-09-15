package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.WooCommerceRepository
import com.example.utils.PersianUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val unitPriceOverride: Double? = null,
    val itemDiscount: Double = 0.0
) {
    val unitPrice: Double
        get() = unitPriceOverride ?: product.activePrice

    val totalPrice: Double
        get() = ((unitPrice * quantity) - itemDiscount).coerceAtLeast(0.0)

    val totalCogs: Double
        get() = product.purchasePrice * quantity
}

enum class PosTab(val title: String) {
    POS("صندوق فروش"),
    INVENTORY("انبار و ووکامرس"),
    ACCOUNTING("حسابداری و صندوق"),
    CUSTOMERS("مشتریان و چک‌ها"),
    SETTINGS("تنظیمات")
}

data class PosUiState(
    val activeTab: PosTab = PosTab.POS,
    val searchQuery: String = "",
    val selectedCategory: String = "همه",
    val cart: List<CartItem> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val invoiceDiscountPercent: Double = 0.0,
    val invoiceDiscountFixed: Double = 0.0,
    val applyTax: Boolean = true,
    val paymentMethod: String = "CASH", // CASH, POS_CARD, CREDIT, CHEQUE, SPLIT
    val cashReceived: Double = 0.0,
    val cardRrn: String = "",
    val cardTerminalName: String = "به‌پرداخت ملت",
    val chequeSayad: String = "",
    val chequeBank: String = "ملت",
    val chequeDueDate: String = "",
    val splitCash: Double = 0.0,
    val splitCard: Double = 0.0,
    val splitCredit: Double = 0.0,
    val lastCompletedOrder: OrderEntity? = null,
    val showReceiptDialog: Boolean = false,
    val isSyncingWoo: Boolean = false,
    val syncMessage: String? = null,
    val showCalculatorDialog: Boolean = false,
    val showQuickItemDialog: Boolean = false,
    val showCloseSessionDialog: Boolean = false
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = WooCommerceRepository(db)

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cheques: StateFlow<List<ChequeEntity>> = repository.allCheques
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSession: StateFlow<CashRegisterSessionEntity?> = repository.currentSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val storeConfig: StateFlow<StoreConfigEntity?> = repository.storeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectTab(tab: PosTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    // Cart operations
    fun addToCart(product: ProductEntity) {
        _uiState.update { state ->
            val existing = state.cart.find { it.product.id == product.id }
            val newCart = if (existing != null) {
                state.cart.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1)
                    else it
                }
            } else {
                state.cart + CartItem(product = product, quantity = 1)
            }
            state.copy(cart = newCart)
        }
    }

    fun removeFromCart(productId: Long) {
        _uiState.update { state ->
            state.copy(cart = state.cart.filterNot { it.product.id == productId })
        }
    }

    fun updateCartItemQuantity(productId: Long, delta: Int) {
        _uiState.update { state ->
            val newCart = state.cart.mapNotNull { item ->
                if (item.product.id == productId) {
                    val newQty = item.quantity + delta
                    if (newQty > 0) item.copy(quantity = newQty) else null
                } else item
            }
            state.copy(cart = newCart)
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(
                cart = emptyList(),
                selectedCustomer = null,
                invoiceDiscountPercent = 0.0,
                invoiceDiscountFixed = 0.0,
                cashReceived = 0.0,
                cardRrn = ""
            )
        }
    }

    fun setCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setCashReceived(amount: Double) {
        _uiState.update { it.copy(cashReceived = amount) }
    }

    fun setCardDetails(rrn: String, terminal: String) {
        _uiState.update { it.copy(cardRrn = rrn, cardTerminalName = terminal) }
    }

    fun setChequeDetails(sayad: String, bank: String, dueDate: String) {
        _uiState.update { it.copy(chequeSayad = sayad, chequeBank = bank, chequeDueDate = dueDate) }
    }

    fun setInvoiceDiscount(percent: Double, fixed: Double) {
        _uiState.update { it.copy(invoiceDiscountPercent = percent, invoiceDiscountFixed = fixed) }
    }

    fun toggleTax(enabled: Boolean) {
        _uiState.update { it.copy(applyTax = enabled) }
    }

    fun addCustomItem(name: String, price: Double, category: String = "خدمات و متفرقه") {
        val tempProduct = ProductEntity(
            id = -System.currentTimeMillis(),
            name = name,
            regularPrice = price,
            purchasePrice = price * 0.5,
            category = category,
            stockQuantity = 999,
            manageStock = false,
            syncStatus = "LOCAL"
        )
        addToCart(tempProduct)
        _uiState.update { it.copy(showQuickItemDialog = false) }
    }

    fun toggleCalculator(show: Boolean) {
        _uiState.update { it.copy(showCalculatorDialog = show) }
    }

    fun toggleQuickItemDialog(show: Boolean) {
        _uiState.update { it.copy(showQuickItemDialog = show) }
    }

    fun dismissReceipt() {
        _uiState.update { it.copy(showReceiptDialog = false) }
    }

    // Checkout & Order Finalization
    fun completeSale() {
        val state = _uiState.value
        if (state.cart.isEmpty()) return

        val config = storeConfig.value ?: StoreConfigEntity()
        val taxRate = if (state.applyTax) (config.taxRatePercent / 100.0) else 0.0

        val subtotal = state.cart.sumOf { it.totalPrice }
        val discount = (subtotal * (state.invoiceDiscountPercent / 100.0)) + state.invoiceDiscountFixed
        val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
        val tax = afterDiscount * taxRate
        val totalNet = afterDiscount + tax
        val totalCogs = state.cart.sumOf { it.totalCogs }
        val grossProfit = totalNet - totalCogs

        val invoiceNumber = "INV-${PersianUtils.getCurrentPersianDate().replace("/", "")}-${(1000..9999).random()}"

        val order = OrderEntity(
            invoiceNumber = invoiceNumber,
            customerName = state.selectedCustomer?.name ?: "مشتری حضوری",
            customerPhone = state.selectedCustomer?.phone ?: "",
            customerId = state.selectedCustomer?.id,
            subtotal = subtotal,
            discountAmount = discount,
            taxAmount = tax,
            netAmount = totalNet,
            cogsAmount = totalCogs,
            grossProfit = grossProfit,
            paymentMethod = state.paymentMethod,
            cashPaid = when (state.paymentMethod) {
                "CASH" -> totalNet
                "SPLIT" -> state.splitCash
                else -> 0.0
            },
            cardPaid = when (state.paymentMethod) {
                "POS_CARD" -> totalNet
                "SPLIT" -> state.splitCard
                else -> 0.0
            },
            creditPaid = when (state.paymentMethod) {
                "CREDIT" -> totalNet
                "SPLIT" -> state.splitCredit
                else -> 0.0
            },
            chequePaid = if (state.paymentMethod == "CHEQUE") totalNet else 0.0,
            cardRrn = if (state.paymentMethod == "POS_CARD") state.cardRrn else null,
            cardTerminal = if (state.paymentMethod == "POS_CARD") state.cardTerminalName else null,
            status = if (config.isWooConnected) "PENDING_SYNC" else "COMPLETED",
            notes = "صندوقدار: ${config.defaultCashier}",
            itemsJson = state.cart.joinToString(";") { "${it.product.name} x${it.quantity} = ${it.totalPrice.toLong()}" }
        )

        viewModelScope.launch {
            val orderId = db.orderDao().insertOrder(order)
            val savedOrder = order.copy(orderId = orderId)

            // 1. Decrease local stocks
            for (item in state.cart) {
                if (item.product.id > 0) {
                    db.productDao().decreaseStock(item.product.id, item.quantity)
                }
            }

            // 2. If Credit (نسیه), update customer balance and ledger
            if ((state.paymentMethod == "CREDIT" || state.paymentMethod == "SPLIT") && state.selectedCustomer != null) {
                val creditAmount = if (state.paymentMethod == "CREDIT") totalNet else state.splitCredit
                if (creditAmount > 0) {
                    db.customerDao().updateCustomerBalance(state.selectedCustomer.id, creditAmount)
                    db.customerLedgerDao().insertLedger(
                        CustomerLedgerEntity(
                            customerId = state.selectedCustomer.id,
                            type = "DEBIT",
                            amount = creditAmount,
                            orderId = orderId,
                            description = "خرید نسیه فاکتور شماره $invoiceNumber"
                        )
                    )
                }
            }

            // 3. If Cheque (چک صیادی), register cheque
            if (state.paymentMethod == "CHEQUE" && state.chequeSayad.isNotBlank()) {
                db.chequeDao().insertCheque(
                    ChequeEntity(
                        sayadNumber = state.chequeSayad,
                        bankName = state.chequeBank,
                        accountOwner = state.selectedCustomer?.name ?: "مشتری",
                        amount = totalNet,
                        dueDateJalali = state.chequeDueDate.ifBlank { PersianUtils.getCurrentPersianDate() },
                        dueTimestamp = System.currentTimeMillis() + 30L * 86400000L,
                        customerName = state.selectedCustomer?.name ?: "مشتری حضوری",
                        orderId = orderId
                    )
                )
            }

            // 4. Submit order to WooCommerce if connected
            val lineItems = state.cart.filter { it.product.id > 0 }.map { Pair(it.product.id, it.quantity) }
            if (config.isWooConnected && config.autoSyncOrders) {
                repository.submitOrderToWoo(savedOrder, lineItems)
            }

            _uiState.update {
                it.copy(
                    lastCompletedOrder = savedOrder,
                    showReceiptDialog = true,
                    cart = emptyList(),
                    selectedCustomer = null,
                    invoiceDiscountPercent = 0.0,
                    invoiceDiscountFixed = 0.0,
                    cashReceived = 0.0,
                    cardRrn = "",
                    chequeSayad = ""
                )
            }
        }
    }

    // Sync from WooCommerce
    fun syncWithWooCommerce() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWoo = true, syncMessage = "در حال دریافت محصولات از ووکامرس...") }
            val result = repository.syncProductsFromWoo()
            _uiState.update { it.copy(isSyncingWoo = false, syncMessage = result.second) }
        }
    }

    fun testWooConnection(url: String, key: String, secret: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWoo = true) }
            val res = repository.testWooConnection(url, key, secret)
            _uiState.update { it.copy(isSyncingWoo = false) }
            onResult(res.first, res.second)
        }
    }

    fun saveWooSettings(url: String, key: String, secret: String, storeName: String, storePhone: String, storeAddress: String, footerNote: String, taxPercent: Double) {
        viewModelScope.launch {
            val current = storeConfig.value ?: StoreConfigEntity()
            val updated = current.copy(
                wooUrl = url.trim(),
                consumerKey = key.trim(),
                consumerSecret = secret.trim(),
                storeName = storeName.trim(),
                storePhone = storePhone.trim(),
                storeAddress = storeAddress.trim(),
                invoiceFooterNote = footerNote.trim(),
                taxRatePercent = taxPercent,
                isWooConnected = url.isNotBlank() && key.isNotBlank()
            )
            db.storeConfigDao().insertOrUpdateConfig(updated)
        }
    }

    // Quick stock & price edit
    fun updateProductStockAndPrice(id: Long, newStock: Int, newPrice: Double) {
        viewModelScope.launch {
            db.productDao().quickUpdateStockAndPrice(id, newStock, newPrice)
            val config = storeConfig.value
            if (config?.isWooConnected == true && id > 0) {
                repository.updateRemoteStock(id, newStock, newPrice)
            }
        }
    }

    // Add new product locally
    fun addNewProduct(name: String, sku: String, barcode: String, price: Double, purchasePrice: Double, stock: Int, category: String) {
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val product = ProductEntity(
                id = id,
                name = name,
                sku = sku.ifBlank { "SKU-$id" },
                barcode = barcode.ifBlank { sku },
                regularPrice = price,
                purchasePrice = purchasePrice,
                stockQuantity = stock,
                category = category.ifBlank { "عمومی" },
                syncStatus = "LOCAL"
            )
            db.productDao().insertProduct(product)
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double, paidFrom: String, description: String = "") {
        viewModelScope.launch {
            db.expenseDao().insertExpense(
                ExpenseEntity(
                    title = title,
                    category = category,
                    amount = amount,
                    paidFrom = paidFrom,
                    description = description
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            db.expenseDao().deleteExpense(expense)
        }
    }

    // Customers
    fun addCustomer(name: String, phone: String, creditLimit: Double = 10_000_000.0, address: String = "") {
        viewModelScope.launch {
            db.customerDao().insertCustomer(
                CustomerEntity(
                    name = name,
                    phone = phone,
                    creditLimit = creditLimit,
                    address = address
                )
            )
        }
    }

    fun settleCustomerDebt(customerId: Long, amount: Double, description: String = "تسویه بدهی حساب") {
        viewModelScope.launch {
            db.customerDao().updateCustomerBalance(customerId, -amount)
            db.customerLedgerDao().insertLedger(
                CustomerLedgerEntity(
                    customerId = customerId,
                    type = "CREDIT",
                    amount = amount,
                    description = description
                )
            )
        }
    }

    // Cheques
    fun updateChequeStatus(id: Long, status: String) {
        viewModelScope.launch {
            db.chequeDao().updateChequeStatus(id, status)
        }
    }

    // Cash register session (Z-Report / بستن صندوق)
    fun openNewSession(openingCash: Double, cashierName: String = "صندوقدار") {
        viewModelScope.launch {
            db.cashRegisterDao().insertSession(
                CashRegisterSessionEntity(
                    openingBalance = openingCash,
                    cashierName = cashierName
                )
            )
        }
    }

    fun closeCurrentSession(actualCashCounted: Double) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            // Calculate sales since session opened
            val ordersSince = db.orderDao().getOrdersSince(session.openedAt).firstOrNull() ?: emptyList()
            val cashSales = ordersSince.sumOf { it.cashPaid }
            val cardSales = ordersSince.sumOf { it.cardPaid }
            val creditSales = ordersSince.sumOf { it.creditPaid }
            val chequeSales = ordersSince.sumOf { it.chequePaid }

            val expensesSince = db.expenseDao().getExpensesSince(session.openedAt).firstOrNull() ?: emptyList()
            val cashExpenses = expensesSince.filter { it.paidFrom == "صندوق نقدی" }.sumOf { it.amount }

            val expectedCash = session.openingBalance + cashSales - cashExpenses
            val diff = actualCashCounted - expectedCash

            val closedSession = session.copy(
                closedAt = System.currentTimeMillis(),
                cashSales = cashSales,
                cardSales = cardSales,
                creditSales = creditSales,
                chequeSales = chequeSales,
                expensesPaid = cashExpenses,
                expectedCash = expectedCash,
                actualCashCounted = actualCashCounted,
                difference = diff,
                status = "CLOSED"
            )
            db.cashRegisterDao().updateSession(closedSession)
        }
    }
}
