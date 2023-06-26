package com.example.todoapp.db.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface RevisionDao {

    @Query("SELECT revision.revision FROM revision WHERE revision.id = 1")
    fun getCurrentRevision(): Int

    @Query("UPDATE revision SET revision = :newRevision WHERE revision.id = 1")
    fun updateRevision(newRevision: Int)
}