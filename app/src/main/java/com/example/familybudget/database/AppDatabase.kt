package com.example.familybudget.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        DreamEntity::class,
        ExpenseLimitEntity::class,
        BalanceEntity::class,
        UserPreferencesEntity::class,
        CustomCategoryEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun dreamDao(): DreamDao
    abstract fun expenseLimitDao(): ExpenseLimitDao
    abstract fun balanceDao(): BalanceDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financial_compass_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}