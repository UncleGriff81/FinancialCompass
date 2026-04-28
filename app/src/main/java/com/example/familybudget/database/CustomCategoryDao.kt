package com.example.familybudget.database

import androidx.room.*

@Dao
interface CustomCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(category: CustomCategoryEntity)

    @Update
    fun update(category: CustomCategoryEntity)

    @Delete
    fun delete(category: CustomCategoryEntity)

    @Query("SELECT * FROM custom_categories ORDER BY id ASC")
    fun getAllCategories(): List<CustomCategoryEntity>

    @Query("SELECT * FROM custom_categories WHERE id = :id")
    fun getCategoryById(id: Long): CustomCategoryEntity?
}