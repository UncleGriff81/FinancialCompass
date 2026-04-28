package com.example.familybudget.database

import androidx.room.*

@Dao
interface TransactionDao {
    @Insert
    fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = (SELECT id FROM transactions ORDER BY date DESC LIMIT 1)")
    fun deleteLastTransaction()

    @Update
    fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions")
    fun deleteAll()
}