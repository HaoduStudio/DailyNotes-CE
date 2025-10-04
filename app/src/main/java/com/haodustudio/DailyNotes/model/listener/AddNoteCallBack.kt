package com.haodustudio.DailyNotes.model.listener

interface AddNoteCallBack {
    fun onSuccessful(id: Long)
    fun onFailure(e: Exception)
    fun hasExist()
}