package com.haodustudio.DailyNotes.view.activities.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.haodustudio.DailyNotes.R

open class NoRightSlideActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.NoRightSlideTheme)
        super.onCreate(savedInstanceState)
    }
}