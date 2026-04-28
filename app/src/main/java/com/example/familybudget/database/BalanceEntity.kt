package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balances")
data class BalanceEntity(
    @PrimaryKey
    val id: String = "main",
    val cashBalance: Double,
    val cardBalance: Double
)