package com.example.familybudget.database

import androidx.room.*

@Dao
interface ShoppingItemDao {
    @Insert
    fun insert(item: ShoppingItemEntity)

    @Update
    fun update(item: ShoppingItemEntity)

    @Delete
    fun delete(item: ShoppingItemEntity)

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY id ASC")
    fun getItemsByListId(listId: Long): List<ShoppingItemEntity>

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    fun deleteAllByListId(listId: Long)
}