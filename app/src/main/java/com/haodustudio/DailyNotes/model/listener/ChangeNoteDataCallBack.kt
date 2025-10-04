package com.haodustudio.DailyNotes.model.listener

import com.haodustudio.DailyNotes.model.models.Note

interface ChangeNoteDataCallBack {
    fun doChange(it: Note)
    fun onChangeSuccessful()
    fun onChangeFailure(e: Exception)
}