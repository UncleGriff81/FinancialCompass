package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dreams")
data class DreamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,            // название мечты
    val targetAmount: Double,    // сумма цели
    val savedAmount: Double      // накоплено (пока не используем)
)