package com.example.familybudget.database

import androidx.room.*

@Dao
interface ShoppingListDao {
    @Insert
    fun insert(list: ShoppingListEntity): Long

    @Update
    fun update(list: ShoppingListEntity)

    @Delete
    fun delete(list: ShoppingListEntity)

    @Query("SELECT * FROM shopping_lists ORDER BY date DESC")
    fun getAllLists(): List<ShoppingListEntity>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    fun getListById(id: Long): ShoppingListEntity?
}