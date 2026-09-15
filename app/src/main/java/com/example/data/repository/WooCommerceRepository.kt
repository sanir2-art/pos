package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class WooCommerceRepository(private val db: AppDatabase) {

    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val allOrders: Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    val allCheques: Flow<List<ChequeEntity>> = db.chequeDao().getAllCheques()
    val currentSession: Flow<CashRegisterSessionEntity?> = db.cashRegisterDao().getCurrentOpenSession()
    val storeConfig: Flow<StoreConfigEntity?> = db.storeConfigDao().getConfig()

    private var cachedApi: WooCommerceApi? = null

    private suspend fun getApi(): WooCommerceApi? {
        val config = db.storeConfigDao().getConfigDirect() ?: return null
        if (config.wooUrl.isBlank() || config.consumerKey.isBlank() || config.consumerSecret.isBlank()) {
            return null
        }
        return try {
            cachedApi ?: WooCommerceClient.createApi(
                baseUrl = config.wooUrl.trim(),
                consumerKey = config.consumerKey.trim(),
                consumerSecret = config.consumerSecret.trim()
            ).also { cachedApi = it }
        } catch (e: Exception) {
            Log.e("WooCommerceRepo", "Error creating API", e)
            null
        }
    }

    suspend fun testWooConnection(url: String, key: String, secret: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val testApi = WooCommerceClient.createApi(url.trim(), key.trim(), secret.trim())
            val response = testApi.testConnection(1)
            if (response.isSuccessful) {
                cachedApi = testApi
                Pair(true, "اتصال به ووکامرس با موفقیت برقرار شد!")
            } else {
                Pair(false, "خطا در ارتباط: کد وضعیت ${response.code()} - لطفاً آدرس سایت و کلیدها را بررسی کنید.")
            }
        } catch (e: Exception) {
            Pair(false, "عدم برقراری ارتباط: ${e.localizedMessage ?: "خطای ناشناخته شبکه"}")
        }
    }

    suspend fun syncProductsFromWoo(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val api = getApi()
        if (api == null) {
            return@withContext Pair(false, "اطلاعات اتصال به ووکامرس (آدرس سایت، Consumer Key و Consumer Secret) در بخش تنظیمات وارد نشده است.")
        }
        try {
            val remoteProducts = api.getProducts(perPage = 100)
            val entities = remoteProducts.map { p ->
                val regPrice = p.regularPrice?.toDoubleOrNull() ?: p.price?.toDoubleOrNull() ?: 0.0
                val sPrice = p.salePrice?.toDoubleOrNull()
                val qty = p.stockQuantity ?: 0
                // For purchase price (بهای تمام شده), default to 70% of regular price for margin calculation if not set
                val existing = db.productDao().getProductById(p.id)
                val purchase = existing?.purchasePrice ?: (regPrice * 0.70)
                val categoryName = p.categories?.firstOrNull()?.name ?: "محصولات سایت"
                val img = p.images?.firstOrNull()?.src

                ProductEntity(
                    id = p.id,
                    name = p.name,
                    sku = p.sku ?: "SKU-${p.id}",
                    barcode = p.sku ?: "${p.id}",
                    regularPrice = regPrice,
                    salePrice = sPrice,
                    purchasePrice = purchase,
                    stockQuantity = qty,
                    manageStock = p.manageStock ?: true,
                    category = categoryName,
                    imageUrl = img,
                    syncStatus = "SYNCED",
                    updatedAt = System.currentTimeMillis()
                )
            }
            db.productDao().insertProducts(entities)

            // Update last sync time
            val config = db.storeConfigDao().getConfigDirect()
            if (config != null) {
                db.storeConfigDao().insertOrUpdateConfig(config.copy(lastSyncTime = System.currentTimeMillis(), isWooConnected = true))
            }
            Pair(true, "${entities.size} محصول با موفقیت از ووکامرس دریافت و همگام شد.")
        } catch (e: Exception) {
            Log.e("WooCommerceRepo", "Sync products failed", e)
            Pair(false, "خطا در همگام‌سازی: ${e.localizedMessage}")
        }
    }

    suspend fun submitOrderToWoo(order: OrderEntity, lineItems: List<Pair<Long, Int>>): Boolean = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext false
        try {
            val wcLineItems = lineItems.map { (prodId, qty) ->
                WcLineItem(productId = prodId, quantity = qty)
            }
            val request = WcCreateOrderRequest(
                paymentMethod = when (order.paymentMethod) {
                    "POS_CARD" -> "pos_card"
                    "CREDIT" -> "store_credit"
                    "CHEQUE" -> "cheque"
                    else -> "cash_in_store"
                },
                paymentMethodTitle = when (order.paymentMethod) {
                    "POS_CARD" -> "کارت‌خوان حضوری"
                    "CREDIT" -> "نسیه / حساب دفتری"
                    "CHEQUE" -> "چک صیادی"
                    else -> "فروش نقدی حضوری"
                },
                setPaid = order.paymentMethod != "CREDIT",
                status = "completed",
                billing = WcBilling(
                    firstName = order.customerName,
                    phone = order.customerPhone,
                    address_1 = "فروش حضوری در فروشگاه"
                ),
                lineItems = wcLineItems,
                customerNote = "شماره فاکتور: ${order.invoiceNumber} | ${order.notes}"
            )
            val response = api.createOrder(request)
            db.orderDao().markOrderSynced(order.orderId, response.id)
            true
        } catch (e: Exception) {
            Log.e("WooCommerceRepo", "Order submission failed", e)
            false
        }
    }

    suspend fun updateRemoteStock(productId: Long, newStock: Int, newPrice: Double? = null) = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext
        try {
            api.updateProduct(
                id = productId,
                body = WcUpdateProductStockRequest(
                    stockQuantity = newStock,
                    regularPrice = newPrice?.toLong()?.toString()
                )
            )
            val p = db.productDao().getProductById(productId)
            if (p != null) {
                db.productDao().updateProduct(p.copy(syncStatus = "SYNCED"))
            }
        } catch (e: Exception) {
            Log.e("WooCommerceRepo", "Update stock failed", e)
        }
    }

    suspend fun syncPendingOrders(): Int = withContext(Dispatchers.IO) {
        val pending = db.orderDao().getPendingSyncOrders()
        var successCount = 0
        for (order in pending) {
            // Reconstruct items if needed
            val dummyList = emptyList<Pair<Long, Int>>() // Or parse itemsJson
            if (submitOrderToWoo(order, dummyList)) {
                successCount++
            }
        }
        successCount
    }

    // Initialize mock demo data if app launched for first time without products
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val config = db.storeConfigDao().getConfigDirect()
        if (config == null) {
            db.storeConfigDao().insertOrUpdateConfig(StoreConfigEntity())
        }

        val existingProducts = db.productDao().getAllProducts().firstOrNull()
        if (existingProducts.isNullOrEmpty()) {
            val initialProducts = listOf(
                ProductEntity(
                    id = 101,
                    name = "زعفران یک مثقالی قائنات درجه یک",
                    sku = "ZAF-01",
                    barcode = "62601001",
                    regularPrice = 450_000.0,
                    purchasePrice = 320_000.0,
                    stockQuantity = 45,
                    category = "مواد غذایی و سوغات",
                    imageUrl = "https://images.unsplash.com/photo-1615485500704-8e990f9900f7?w=400"
                ),
                ProductEntity(
                    id = 102,
                    name = "دانه قهوه اسپرسو ۱۰۰٪ عربیکا (۱ کیلو)",
                    sku = "COF-AR1",
                    barcode = "62601002",
                    regularPrice = 680_000.0,
                    purchasePrice = 490_000.0,
                    stockQuantity = 28,
                    category = "نوشیدنی و قهوه",
                    imageUrl = "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=400"
                ),
                ProductEntity(
                    id = 103,
                    name = "عسل طبیعی سبلان کوهستانی (۹۰۰ گرم)",
                    sku = "HON-SAB",
                    barcode = "62601003",
                    regularPrice = 390_000.0,
                    salePrice = 350_000.0,
                    purchasePrice = 240_000.0,
                    stockQuantity = 19,
                    category = "مواد غذایی و سوغات",
                    imageUrl = "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400"
                ),
                ProductEntity(
                    id = 104,
                    name = "پاوربانک ۲۰ هزار فست‌شارژ شیائومی",
                    sku = "PB-XIA20",
                    barcode = "62601004",
                    regularPrice = 1_450_000.0,
                    purchasePrice = 1_100_000.0,
                    stockQuantity = 14,
                    category = "دیجیتال و الکترونیک",
                    imageUrl = "https://images.unsplash.com/photo-1609592424369-02684826b14f?w=400"
                ),
                ProductEntity(
                    id = 105,
                    name = "هدفون بی‌سیم بلوتوثی نویزکنسلینگ",
                    sku = "HP-BT900",
                    barcode = "62601005",
                    regularPrice = 2_100_000.0,
                    purchasePrice = 1_550_000.0,
                    stockQuantity = 8,
                    category = "دیجیتال و الکترونیک",
                    imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400"
                ),
                ProductEntity(
                    id = 106,
                    name = "کیف پول چرم طبیعی دست‌دوز مردانه",
                    sku = "LEA-WLT",
                    barcode = "62601006",
                    regularPrice = 520_000.0,
                    purchasePrice = 310_000.0,
                    stockQuantity = 22,
                    category = "پوشاک و اکسسوری",
                    imageUrl = "https://images.unsplash.com/photo-1627123424574-724758594e93?w=400"
                ),
                ProductEntity(
                    id = 107,
                    name = "پیراهن آستین بلند کتان پاییزه",
                    sku = "CLO-SHR1",
                    barcode = "62601007",
                    regularPrice = 790_000.0,
                    purchasePrice = 520_000.0,
                    stockQuantity = 35,
                    category = "پوشاک و اکسسوری",
                    imageUrl = "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400"
                ),
                ProductEntity(
                    id = 108,
                    name = "روغن زیتون فرابکر رودبار (۱ لیتری)",
                    sku = "OIL-ROD",
                    barcode = "62601008",
                    regularPrice = 420_000.0,
                    purchasePrice = 295_000.0,
                    stockQuantity = 40,
                    category = "مواد غذایی و سوغات",
                    imageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400"
                )
            )
            db.productDao().insertProducts(initialProducts)

            // Seed sample customers
            val initialCustomers = listOf(
                CustomerEntity(
                    name = "محمد اکبری",
                    phone = "09121112233",
                    balance = 750_000.0, // بدهکار
                    creditLimit = 15_000_000.0,
                    address = "تهران، سهروردی شمالی"
                ),
                CustomerEntity(
                    name = "سارا حسینی",
                    phone = "09359876543",
                    balance = 0.0,
                    creditLimit = 10_000_000.0,
                    address = "تهران، شهرک غرب"
                ),
                CustomerEntity(
                    name = "علیرضا رادمنش",
                    phone = "09194445566",
                    balance = 1_850_000.0, // بدهکار
                    creditLimit = 25_000_000.0,
                    address = "تهران، بازار بزرگ"
                )
            )
            for (c in initialCustomers) {
                db.customerDao().insertCustomer(c)
            }

            // Seed sample expenses
            val initialExpenses = listOf(
                ExpenseEntity(
                    title = "شارژ ساختمان و نظافت پاساژ",
                    category = "قبوض",
                    amount = 350_000.0,
                    paidFrom = "صندوق نقدی",
                    timestamp = System.currentTimeMillis() - 86400000L
                ),
                ExpenseEntity(
                    title = "خرید نایلون دسته‌دار و کارتن بسته‌بندی",
                    category = "بسته‌بندی",
                    amount = 480_000.0,
                    paidFrom = "کارت بانکی",
                    timestamp = System.currentTimeMillis() - 43200000L
                )
            )
            for (e in initialExpenses) {
                db.expenseDao().insertExpense(e)
            }

            // Seed sample cheques
            val sampleCheque = ChequeEntity(
                sayadNumber = "7829104859201948",
                bankName = "بانک صادرات ایران",
                accountOwner = "علیرضا رادمنش",
                amount = 3_500_000.0,
                dueDateJalali = "۱۴۰۳/۰۷/۱۰",
                dueTimestamp = System.currentTimeMillis() + (15L * 86400000L),
                status = "PENDING",
                customerName = "علیرضا رادمنش",
                notes = "بابت تسویه فاکتور خرید عمده"
            )
            db.chequeDao().insertCheque(sampleCheque)

            // Open cash register session if none exists
            val openSession = db.cashRegisterDao().getCurrentOpenSession().firstOrNull()
            if (openSession == null) {
                db.cashRegisterDao().insertSession(
                    CashRegisterSessionEntity(
                        openingBalance = 500_000.0,
                        cashierName = "مدیر فروشگاه"
                    )
                )
            }
        }
    }
}
