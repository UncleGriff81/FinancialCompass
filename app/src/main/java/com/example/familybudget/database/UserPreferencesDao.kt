package com.example.familybudget.database

import androidx.room.*

@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(prefs: UserPreferencesEntity)

    @Query("SELECT * FROM user_preferences WHERE id = :id")
    fun getPreferencesSync(id: String): UserPreferencesEntity?

    @Update
    fun update(prefs: UserPreferencesEntity): Int
}