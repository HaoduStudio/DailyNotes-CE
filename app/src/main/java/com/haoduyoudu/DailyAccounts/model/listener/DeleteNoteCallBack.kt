package com.haodustudio.DailyNotes.model.listener

interface DeleteNoteCallBack {
    fun onSuccessful()
    fun onFailure(e: Exception)
}