package com.haodustudio.DailyNotes.view.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.databinding.ActivityMoodCalendarBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V1
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V2
import com.haodustudio.DailyNotes.model.models.Note
import com.haodustudio.DailyNotes.utils.DateUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.adapters.MoodCalendarAdapter
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import java.util.Calendar

class MoodCalendar : BaseActivity() {

    private val binding by lazy { ActivityMoodCalendarBinding.inflate(layoutInflater) }
    private val appViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }

    private lateinit var calendarAdapter: MoodCalendarAdapter
    private val moodList = ArrayList<MoodCalendarAdapter.MoodItem>()
    private val calendar = Calendar.getInstance()
    private var allNotes = listOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        initObservers()
        setupClickListeners()
    }

    private fun initViews() {
        calendarAdapter = MoodCalendarAdapter(this, moodList)
        binding.calendar.apply {
            layoutManager = GridLayoutManager(this@MoodCalendar, 7)
            adapter = calendarAdapter
        }
    }

    private fun initObservers() {
        appViewModel.notesList.observe(this) {
            allNotes = it
            updateCalendar()
        }
    }

    private fun setupClickListeners() {
        binding.leftButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        binding.rightButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        calendarAdapter.setOnItemClickListener { _, position ->
            val clickedItem = moodList[position]
            handleDayClick(clickedItem, position + 1)
        }

        calendarAdapter.setOnItemLongClickListener { _, position ->
            val clickedItem = moodList[position]
            if (clickedItem.noteId != -1L) {
                launchNoteOptions(clickedItem.noteId, clickedItem.type)
            }
            true
        }
    }

    private fun handleDayClick(item: MoodCalendarAdapter.MoodItem, dayOfMonth: Int) {
        if (item.noteId != -1L) {
            val intent = when (item.type) {
                NOTE_TYPE_V1 -> Intent(this, NoteViewer::class.java)
                NOTE_TYPE_V2 -> Intent(this, FreeMakeNote::class.java)
                else -> null
            }
            intent?.putExtra("noteId", item.noteId)
            startActivity(intent)
        } else {
            val today = Calendar.getInstance()
            val clickedDate = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayOfMonth) }
            if (!clickedDate.after(today)) {
                Intent(this, NoteChangeMood::class.java).apply {
                    putExtra("firstWrite", true)
                    putExtra("yy", calendar.get(Calendar.YEAR))
                    putExtra("mm", calendar.get(Calendar.MONTH) + 1)
                    putExtra("dd", dayOfMonth)
                    startActivity(this)
                }
            }
        }
    }

    private fun launchNoteOptions(noteId: Long, type: Int) {
        Intent(this, NoteOption::class.java).apply {
            putExtra("noteId", noteId)
            putExtra("type", type)
            startActivity(this)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCalendar() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        binding.mouth.text = DateUtils.formatYYMMDD(year, month, 1, "yyyy/MM")

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val notesInMonth = allNotes.filter { it.yy == year && it.mm == month }
            .associateBy { it.dd }

        moodList.clear()
        for (day in 1..daysInMonth) {
            val note = notesInMonth[day]
            val moodItem = if (note != null) {
                MoodCalendarAdapter.MoodItem(note.mood.first, note.id, note.type)
            } else {
                MoodCalendarAdapter.MoodItem()
            }
            moodList.add(moodItem)
        }
        calendarAdapter.notifyDataSetChanged()
    }
}