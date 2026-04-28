package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val isChecked: Boolean = false
)