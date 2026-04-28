package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_limits")
data class ExpenseLimitEntity(
    @PrimaryKey
    val category: String,        // "utility", "food", "transport", "communication", "health", "education"
    val limitAmount: Double,     // лимит
    val remainingAmount: Double  // остаток
)