package com.example.familybudget.managers

import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class TransactionManager(private val db: AppDatabase) {

    suspend fun addTransaction(
        type: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        balanceAfter: Double
    ) {
        val transaction = TransactionEntity(
            type = type,
            category = category,
            amount = amount,
            paymentMethod = paymentMethod,
            date = Date(),
            balanceAfter = balanceAfter
        )
        withContext(Dispatchers.IO) {
            db.transactionDao().insert(transaction)
        }
    }

    suspend fun updateTransaction(oldTransaction: TransactionEntity, newAmount: Double, newCategory: String, newPaymentMethod: String, newBalanceAfter: Double) {
        val updatedTransaction = oldTransaction.copy(
            amount = newAmount,
            category = newCategory,
            paymentMethod = newPaymentMethod,
            balanceAfter = newBalanceAfter
        )
        withContext(Dispatchers.IO) {
            db.transactionDao().update(updatedTransaction)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            db.transactionDao().deleteTransaction(transaction.id)
        }
    }

    suspend fun deleteLastTransaction() {
        withContext(Dispatchers.IO) {
            db.transactionDao().deleteLastTransaction()
        }
    }

    suspend fun getAllTransactions(): List<TransactionEntity> {
        return withContext(Dispatchers.IO) {
            db.transactionDao().getAllTransactions()
        }
    }
}