package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long, // WooCommerce ID or local timestamp ID
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val regularPrice: Double = 0.0,
    val salePrice: Double? = null,
    val purchasePrice: Double = 0.0, // بهای تمام شده خرید برای حسابداری
    val stockQuantity: Int = 0,
    val manageStock: Boolean = true,
    val category: String = "عمومی",
    val imageUrl: String? = null,
    val syncStatus: String = "SYNCED", // SYNCED, PENDING_UPDATE, LOCAL
    val updatedAt: Long = System.currentTimeMillis()
) {
    val activePrice: Double
        get() = salePrice ?: regularPrice
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Long = 0,
    val wcOrderId: Long? = null,
    val invoiceNumber: String,
    val customerName: String = "مشتری حضوری",
    val customerPhone: String = "",
    val customerId: Long? = null,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val cogsAmount: Double = 0.0, // جمع بهای تمام شده کالاها
    val grossProfit: Double = 0.0, // سود ناخالص = خالص فروش منهای بهای تمام شده
    val paymentMethod: String = "CASH", // CASH, POS_CARD, CREDIT, CHEQUE, SPLIT
    val cashPaid: Double = 0.0,
    val cardPaid: Double = 0.0,
    val creditPaid: Double = 0.0,
    val chequePaid: Double = 0.0,
    val cardRrn: String? = null, // شماره پیگیری کارتخوان
    val cardTerminal: String? = null, // نام بانک / پایانه کارتخوان
    val status: String = "COMPLETED", // COMPLETED, PENDING_SYNC, REFUNDED
    val notes: String = "",
    val itemsJson: String = "", // JSON representation of purchased items
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val balance: Double = 0.0, // مانده بدهکاری: مثبت یعنی بدهکار است، منفی یعنی بستانکار
    val creditLimit: Double = 10_000_000.0, // سقف اعتبار نسیه
    val nationalId: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_ledgers")
data class CustomerLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String, // "DEBIT" (خرید نسیه), "CREDIT" (پرداخت/تسویه), "DISCOUNT" (تخفیف دستی)
    val amount: Double,
    val orderId: Long? = null,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // اجاره, حقوق, قبوض, حمل و نقل, بسته‌بندی, پذیرایی, تعمیرات, متفرقه
    val amount: Double,
    val paidFrom: String = "صندوق نقدی", // صندوق نقدی, کارت بانکی, تنخواه
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cheques")
data class ChequeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sayadNumber: String, // شناسه ۱۶ رقمی صیاد
    val bankName: String, // بانک ملت، صادرات، ملی، پاسارگاد و...
    val accountOwner: String,
    val amount: Double,
    val dueDateJalali: String, // مثلا ۱۴۰۳/۰۸/۲۰
    val dueTimestamp: Long,
    val status: String = "PENDING", // PENDING (در انتظار), PASSED (وصول شده), BOUNCED (برگشتی), TRANSFERRED (خرج شده)
    val customerName: String,
    val orderId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_register_sessions")
data class CashRegisterSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingBalance: Double = 0.0, // موجودی اولیه صندوق (پول خرد)
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val chequeSales: Double = 0.0,
    val expensesPaid: Double = 0.0,
    val expectedCash: Double = 0.0, // موجودی مورد انتظار = اولیه + فروش نقدی - هزینه‌های نقدی
    val actualCashCounted: Double? = null, // شمارش فیزیکی صندوقدار
    val difference: Double? = null, // کسری (منفی) یا مازاد (مثبت)
    val cashierName: String = "صندوقدار اصلی",
    val status: String = "OPEN" // OPEN, CLOSED
)

@Entity(tableName = "store_config")
data class StoreConfigEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "فروشگاه آنلاین و حضوری من",
    val storePhone: String = "۰۲۱-۸۸۸۸۸۸۸۸",
    val storeAddress: String = "تهران، خیابان ولیعصر، پلاک ۱۰۰",
    val invoiceFooterNote: String = "از خرید و اعتماد شما سپاسگزاریم. اجناس فروخته شده تا ۴۸ ساعت تعویض می‌گردد.",
    val wooUrl: String = "https://myshop.com",
    val consumerKey: String = "",
    val consumerSecret: String = "",
    val isWooConnected: Boolean = false,
    val taxRatePercent: Double = 10.0, // مالیات بر ارزش افزوده (۱۰٪)
    val currency: String = "تومان",
    val defaultCashier: String = "صندوقدار ۱",
    val autoSyncOrders: Boolean = true,
    val lastSyncTime: Long = 0L
)
