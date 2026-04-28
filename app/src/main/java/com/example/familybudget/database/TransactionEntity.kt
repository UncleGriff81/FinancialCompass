package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "transactions")
@TypeConverters(Converters::class)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val date: Date,
    val balanceAfter: Double
)