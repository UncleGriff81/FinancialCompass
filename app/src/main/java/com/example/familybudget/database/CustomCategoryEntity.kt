package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,            // название категории
    val limitAmount: Double,     // лимит
    val remainingAmount: Double  // остаток
)