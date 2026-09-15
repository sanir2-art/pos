package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        CustomerEntity::class,
        CustomerLedgerEntity::class,
        ExpenseEntity::class,
        ChequeEntity::class,
        CashRegisterSessionEntity::class,
        StoreConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerLedgerDao(): CustomerLedgerDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun chequeDao(): ChequeDao
    abstract fun cashRegisterDao(): CashRegisterDao
    abstract fun storeConfigDao(): StoreConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "woopos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
