package com.haodustudio.DailyNotes.helper

import android.widget.Toast
import com.haodustudio.DailyNotes.BaseApplication

fun makeToast(msg: CharSequence) {
    Toast.makeText(BaseApplication.context, msg, Toast.LENGTH_SHORT).show()
}