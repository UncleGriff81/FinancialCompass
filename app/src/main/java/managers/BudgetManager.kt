package com.example.familybudget.managers

import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.BalanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetManager(private val db: AppDatabase) {

    var cashBalance = 0.0
        private set
    var cardBalance = 0.0
        private set

    suspend fun loadBalances() {
        withContext(Dispatchers.IO) {
            val balance = db.balanceDao().getBalance()
            if (balance != null) {
                cashBalance = balance.cashBalance
                cardBalance = balance.cardBalance
            } else {
                db.balanceDao().insert(BalanceEntity(cashBalance = 0.0, cardBalance = 0.0))
            }
        }
    }

    fun addToBalance(amount: Double, paymentMethod: String, isIncome: Boolean) {
        when (paymentMethod) {
            "💵 Наличные" -> if (isIncome) cashBalance += amount else cashBalance -= amount
            "💳 Безналичные" -> if (isIncome) cardBalance += amount else cardBalance -= amount
        }
    }

    fun revertBalanceChange(amount: Double, paymentMethod: String, wasIncome: Boolean) {
        when (paymentMethod) {
            "💵 Наличные" -> if (wasIncome) cashBalance -= amount else cashBalance += amount
            "💳 Безналичные" -> if (wasIncome) cardBalance -= amount else cardBalance += amount
        }
    }

    fun getTotalBalance(): Double = cashBalance + cardBalance

    suspend fun saveBalances() {
        withContext(Dispatchers.IO) {
            val currentBalance = db.balanceDao().getBalance()
            if (currentBalance != null) {
                val updatedBalance = currentBalance.copy(
                    cashBalance = cashBalance,
                    cardBalance = cardBalance
                )
                db.balanceDao().update(updatedBalance)
            }
        }
    }
}