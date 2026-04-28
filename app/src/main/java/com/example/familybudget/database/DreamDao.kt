package com.example.familybudget.database

import androidx.room.*

@Dao
interface DreamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dream: DreamEntity)

    @Query("SELECT * FROM dreams")
    fun getAllDreams(): List<DreamEntity>

    @Query("DELETE FROM dreams")
    fun deleteAll()
}