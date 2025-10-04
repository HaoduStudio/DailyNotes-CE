package com.haodustudio.DailyNotes.view.activities

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.databinding.ActivityNoteChangeMoodBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.helper.toGson
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V1
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V2
import com.haodustudio.DailyNotes.model.listener.AddNoteCallBack
import com.haodustudio.DailyNotes.model.listener.ChangeNoteDataCallBack
import com.haodustudio.DailyNotes.model.models.Note
import com.haodustudio.DailyNotes.utils.DateUtils
import com.haodustudio.DailyNotes.utils.FileUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.adapters.MoodAdapter
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NoteChangeMood : BaseActivity() {

    private val binding by lazy { ActivityNoteChangeMoodBinding.inflate(layoutInflater) }
    private val appViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }
    private var isFirstWrite = false
    private lateinit var note: Note
    private lateinit var moodAdapter: MoodAdapter
    private val moodList = ArrayList<MoodAdapter.MoodItem>()
    private var chooseMoodId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupInitialState()
        loadMoods()
        setupListeners()
    }

    private fun setupInitialState() {
        isFirstWrite = intent.getBooleanExtra("firstWrite", false)
        if (!isFirstWrite) {
            val noteId = intent.getLongExtra("noteId", -1L)
            if (noteId == -1L) {
                makeToast("Empty id")
                finish()
                return
            }
            appViewModel.getNoteFromIdLiveData(noteId).observe(this) {
                note = it
            }
        }
    }

    private fun loadMoods() {
        try {
            val allMoodNames = assets.list(BaseApplication.ASSETS_MOOD_PATH.removeSuffix("/"))?.sorted()
            allMoodNames?.forEach { fileName ->
                val moodId = fileName.substringBefore('.').toIntOrNull()
                if (moodId != null) {
                    moodList.add(MoodAdapter.MoodItem(moodId))
                }
            }
            moodAdapter = MoodAdapter(this, moodList)

            val spanCount = calculateSpanCount()
            binding.recyclerView.apply {
                layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
                adapter = moodAdapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
            makeToast("情绪载入失败")
            finish()
        }
    }

    private fun calculateSpanCount(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val desiredItemWidthDp = 50
        val count = (screenWidthDp / desiredItemWidthDp).toInt()
        return if (count > 0) count else 1
    }

    private fun setupListeners() {
        if (!::moodAdapter.isInitialized) return

        moodAdapter.setOnItemClickListener { _, i ->
            val selectedMood = moodList[i]
            chooseMoodId = selectedMood.moodId
            Glide.with(this).load("file:///android_asset/${BaseApplication.ASSETS_MOOD_PATH}${selectedMood.moodId}.png")
                .into(binding.moodImage)
            if (!isFirstWrite && ::note.isInitialized) {
                binding.moodText.setText(note.mood.second)
            }
            binding.viewFlipper.displayedChild = 1
        }

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.complete.setOnClickListener {
            val moodText = binding.moodText.text.toString().trim()
            when {
                moodText.isEmpty() -> makeToast("输入名字")
                moodText.length > 7 -> makeToast("名字太长了！")
                else -> {
                    if (isFirstWrite) {
                        binding.viewFlipper.displayedChild = 2
                        setupCreationWayListeners(moodText)
                    } else {
                        saveMoodToDataBase(chooseMoodId, moodText)
                    }
                }
            }
        }
    }

    private fun setupCreationWayListeners(moodText: String) {
        var yy = intent.getIntExtra("yy", DateUtils.getYYYYFromCalendar())
        var mm = intent.getIntExtra("mm", DateUtils.getMMFromCalendar())
        var dd = intent.getIntExtra("dd", DateUtils.getDDFromCalendar())

        binding.oldWay.setOnClickListener { createNote(yy, mm, dd, moodText, NOTE_TYPE_V1) }
        binding.newWay.setOnClickListener { createNote(yy, mm, dd, moodText, NOTE_TYPE_V2) }
    }

    private fun createNote(yy: Int, mm: Int, dd: Int, moodText: String, type: Int) {
        val properties = if (type == NOTE_TYPE_V1) {
            hashMapOf(
                "template" to Pair(false, "1").toGson(), "textColor" to "1",
                "noteFolder" to BaseApplication.NOTES_PATH + DateUtils.formatYYMMDD(yy, mm, dd, DateUtils.FORMAT_YYYY_MM_DD) + '/',
                "body" to "", "recordPaths" to arrayListOf<String>().toGson(),
                "imagePaths" to arrayListOf<String>().toGson(), "videoPaths" to arrayListOf<String>().toGson()
            )
        } else {
            hashMapOf(
                "noteFolder" to BaseApplication.NOTES_PATH + DateUtils.formatYYMMDD(yy, mm, dd, DateUtils.FORMAT_YYYY_MM_DD) + '/',
                "backgroundColor" to "12", "pageSize" to "1"
            )
        }
        val newNote = Note(yy, mm, dd, Pair(chooseMoodId, moodText), type, properties)

        lifecycleScope.launch(Dispatchers.IO) {
            appViewModel.addNote(newNote, object : AddNoteCallBack {
                override fun onSuccessful(id: Long) {
                    val intentClass = if (type == NOTE_TYPE_V1) NoteViewer::class.java else FreeMakeNote::class.java
                    val mIntent = Intent(this@NoteChangeMood, intentClass).apply {
                        putExtra("noteId", id)
                        putExtra("editMode", true)
                    }
                    startActivity(mIntent)
                    finish()
                }

                override fun onFailure(e: Exception) {
                    runOnUiThread { makeToast("创建失败") }
                    finish()
                }

                override fun hasExist() {
                    runOnUiThread { makeToast("您已经写过了") }
                    finish()
                }
            })
        }
    }

    private fun saveMoodToDataBase(moodId: Int, moodText: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            appViewModel.changeNoteDataFromId(note.id, object : ChangeNoteDataCallBack {
                override fun doChange(it: Note) {
                    it.mood = Pair(moodId, moodText)
                }

                override fun onChangeSuccessful() {
                    finish()
                }

                override fun onChangeFailure(e: Exception) {
                    runOnUiThread { makeToast("保存失败") }
                    finish()
                }
            })
        }
    }
}