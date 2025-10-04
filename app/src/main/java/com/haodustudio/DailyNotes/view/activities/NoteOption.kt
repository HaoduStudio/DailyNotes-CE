package com.haodustudio.DailyNotes.view.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.databinding.ActivityNoteOptionBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V1
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V2
import com.haodustudio.DailyNotes.model.listener.DeleteNoteCallBack
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.activities.base.DialogActivity
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import kotlin.concurrent.thread

class NoteOption : DialogActivity(noShot = true) {

    private val binding by lazy { ActivityNoteOptionBinding.inflate(layoutInflater) }
    private val appViewModel by lazy { BaseApplication.viewModel as GlobalViewModel }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val noteId = intent.getLongExtra("noteId", -1L)
        val type = intent.getIntExtra("type", NOTE_TYPE_V1)
        if (noteId == -1L) {
            makeToast("Empty id")
            finish()
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.delete.setOnClickListener {
            thread {
                appViewModel.deleteNote(noteId, object : DeleteNoteCallBack {
                    override fun onSuccessful() {
                        finish()
                    }
                    override fun onFailure(e: Exception) {
                        e.printStackTrace()
                        makeToast("Delete error")
                        finish()
                    }
                })
            }
        }

        binding.edit.setOnClickListener {
            val mIntent: Intent = when (type) {
                NOTE_TYPE_V1 -> {
                    Intent(this, NoteViewer::class.java)
                }
                else -> {
                    Intent(this, FreeMakeNote::class.java)
                }
            }
            mIntent.putExtra("noteId", noteId)
            mIntent.putExtra("editMode", true)
            startActivity(mIntent)
            finish()
        }

        binding.share.setOnClickListener {
            val mIntent: Intent = when (type) {
                NOTE_TYPE_V1 -> {
                    Intent(this, NoteViewer::class.java)
                }
                else -> {
                    Intent(this, FreeMakeNote::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                }
            }
            mIntent.putExtra("noteId", noteId)
            mIntent.putExtra("share", true)
            startActivity(mIntent)
            finish()
        }
    }
}