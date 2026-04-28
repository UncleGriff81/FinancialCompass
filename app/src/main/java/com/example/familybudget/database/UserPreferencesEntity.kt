package com.example.familybudget.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    var id: String,
    var isPremium: Boolean,
    var isLifetimeFree: Boolean,
    var installCount: Int,
    var firstInstallDate: Long,
    var trialStartDate: Long
)