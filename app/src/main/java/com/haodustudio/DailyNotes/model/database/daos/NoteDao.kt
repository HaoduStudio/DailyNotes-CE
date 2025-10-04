package com.haodustudio.DailyNotes.model.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.haodustudio.DailyNotes.model.models.Note

@Dao
interface NoteDao {
    @Insert
    fun insertNote(note: Note): Long

    @Query("select * from Note")
    fun loadAllNote(): LiveData<List<Note>>

    @Query("select * from Note where id = (:mId)")
    fun loadNoteFromIdLiveData(mId: Long): LiveData<Note>

    @Query("select * from Note where id = (:mId)")
    fun loadNoteFromId(mId: Long): Note

    @Update
    fun updateNote(newNote: Note)

    @Delete
    fun deleteNote(data: Note)
}