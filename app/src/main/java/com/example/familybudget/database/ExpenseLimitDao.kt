package com.example.familybudget.database

import androidx.room.*

@Dao
interface ExpenseLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(limit: ExpenseLimitEntity)

    @Query("SELECT * FROM expense_limits WHERE category = :category")
    fun getLimit(category: String): ExpenseLimitEntity?

    @Query("SELECT * FROM expense_limits")
    fun getAllLimits(): List<ExpenseLimitEntity>

    @Update
    fun update(limit: ExpenseLimitEntity)

    @Query("UPDATE expense_limits SET remainingAmount = remainingAmount - :amount WHERE category = :category")
    fun decreaseRemaining(category: String, amount: Double)

    @Query("DELETE FROM expense_limits")
    fun deleteAll()
}