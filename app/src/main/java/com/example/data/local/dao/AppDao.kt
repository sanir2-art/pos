package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode OR sku = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :id")
    suspend fun decreaseStock(id: Long, qty: Int)

    @Query("UPDATE products SET stockQuantity = :newStock, regularPrice = :newPrice, syncStatus = 'PENDING_UPDATE', updatedAt = :time WHERE id = :id")
    suspend fun quickUpdateStockAndPrice(id: Long, newStock: Int, newPrice: Double, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getOrdersSince(startTime: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = 'PENDING_SYNC'")
    suspend fun getPendingSyncOrders(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = 'COMPLETED', wcOrderId = :wcId WHERE orderId = :localId")
    suspend fun markOrderSynced(localId: Long, wcId: Long)

    @Query("SELECT COUNT(*) FROM orders")
    fun getOrdersCount(): Flow<Int>

    @Query("SELECT SUM(netAmount) FROM orders WHERE timestamp >= :startTime")
    fun getTotalSalesSince(startTime: Long): Flow<Double?>
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET balance = balance + :amount WHERE id = :id")
    suspend fun updateCustomerBalance(id: Long, amount: Double)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}

@Dao
interface CustomerLedgerDao {
    @Query("SELECT * FROM customer_ledgers WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getLedgersForCustomer(customerId: Long): Flow<List<CustomerLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: CustomerLedgerEntity): Long
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getExpensesSince(startTime: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface ChequeDao {
    @Query("SELECT * FROM cheques ORDER BY dueTimestamp ASC")
    fun getAllCheques(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques WHERE status = 'PENDING' ORDER BY dueTimestamp ASC")
    fun getPendingCheques(): Flow<List<ChequeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheque(cheque: ChequeEntity): Long

    @Query("UPDATE cheques SET status = :status WHERE id = :id")
    suspend fun updateChequeStatus(id: Long, status: String)

    @Delete
    suspend fun deleteCheque(cheque: ChequeEntity)
}

@Dao
interface CashRegisterDao {
    @Query("SELECT * FROM cash_register_sessions WHERE status = 'OPEN' ORDER BY id DESC LIMIT 1")
    fun getCurrentOpenSession(): Flow<CashRegisterSessionEntity?>

    @Query("SELECT * FROM cash_register_sessions ORDER BY id DESC")
    fun getAllSessions(): Flow<List<CashRegisterSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashRegisterSessionEntity): Long

    @Update
    suspend fun updateSession(session: CashRegisterSessionEntity)
}

@Dao
interface StoreConfigDao {
    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<StoreConfigEntity?>

    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): StoreConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: StoreConfigEntity)
}
