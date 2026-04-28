package com.example.familybudget.database

import androidx.room.*

@Dao
interface BalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(balance: BalanceEntity)

    @Query("SELECT * FROM balances WHERE id = 'main'")
    fun getBalance(): BalanceEntity?

    @Update
    fun update(balance: BalanceEntity)
}