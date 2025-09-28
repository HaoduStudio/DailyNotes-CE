package com.haodustudio.DailyNotes.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.databinding.ActivitySearchNoteBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel

class SearchNote : BaseActivity() {
    private val binding by lazy { ActivitySearchNoteBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.complete.setOnClickListener {
            if (binding.editText.length() > 0) {
                val mIntent = Intent(this, ShowFindResult::class.java)
                val list = binding.editText.text.toString().split(" ")
                mIntent.putExtra("keyword", list.toTypedArray())
                startActivity(mIntent)
            }else {
                makeToast("请输入关键词")
            }
        }
    }
}