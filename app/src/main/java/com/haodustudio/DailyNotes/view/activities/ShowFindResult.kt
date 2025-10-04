package com.haodustudio.DailyNotes.view.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityShowFindResultBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V1
import com.haodustudio.DailyNotes.model.models.Note
import com.haodustudio.DailyNotes.utils.DateUtils
import com.haodustudio.DailyNotes.utils.ViewUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.adapters.FindNoteAdapter
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShowFindResult : BaseActivity() {

    private val binding by lazy { ActivityShowFindResultBinding.inflate(layoutInflater) }
    private val appViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }

    private val noteList = ArrayList<FindNoteAdapter.NoteItem>()
    private lateinit var findAdapter: FindNoteAdapter
    private var tempBackgroundPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val keywords = intent.getStringArrayExtra("keyword")
        if (keywords == null || keywords.isEmpty()) {
            makeToast("关键词错误")
            finish()
            return
        }

        initViews(keywords)
        initObservers(keywords)
    }

    private fun initViews(keywords: Array<String>) {
        val titleText = "查找: ${keywords.joinToString(" ")}"
        ViewUtils.ellipsizeEnd(binding.title, 1, titleText)

        findAdapter = FindNoteAdapter(this, noteList)
        findAdapter.setKeyword(keywords)

        binding.listView.apply {
            layoutManager = LinearLayoutManager(this@ShowFindResult)
            adapter = findAdapter
            setEmptyView(binding.emptyView)
        }

        findAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                refreshBackground()
            }
        })

        findAdapter.setOnItemClickListener { _, i ->
            val note = noteList.getOrNull(i) ?: return@setOnItemClickListener
            startActivity(Intent(this, NoteViewer::class.java).putExtra("noteId", note.data.id))
        }

        findAdapter.setOnItemLongClickListener { _, i ->
            val note = noteList.getOrNull(i) ?: return@setOnItemLongClickListener false
            startActivity(Intent(this, NoteOption::class.java).apply {
                putExtra("noteId", note.data.id)
                putExtra("type", note.data.type)
            })
            true
        }
    }

    private fun initObservers(keywords: Array<String>) {
        appViewModel.notesList.observe(this) { allNotes ->
            makeToast("查找中...")
            performSearch(allNotes, keywords)
        }

        appViewModel.appBackgroundPath.observe(this) { path ->
            tempBackgroundPath = path
            refreshBackground()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun performSearch(allNotes: List<Note>, keywords: Array<String>) {
        lifecycleScope.launch(Dispatchers.Default) {
            val sortedNotes = allNotes.sortedByDescending {
                DateUtils.formatYYMMDD(it.yy, it.mm, it.dd, DateUtils.FORMAT_YYYY_MM_DD)
            }

            val results = sortedNotes.filter { note ->
                note.type == NOTE_TYPE_V1 && keywords.any { keyword ->
                    note.data["body"]!!.contains(keyword, ignoreCase = true) ||
                            note.mood.second.contains(keyword, ignoreCase = true)
                }
            }.map { FindNoteAdapter.NoteItem(it) }

            withContext(Dispatchers.Main) {
                noteList.clear()
                noteList.addAll(results)
                findAdapter.notifyDataSetChanged()
                makeToast("找到 ${results.size} 个手帐")
            }
        }
    }

    private fun refreshBackground() {
        if (noteList.isNotEmpty() && tempBackgroundPath.isNotEmpty()) {
            Glide.with(this).load(tempBackgroundPath).into(binding.backgroundImg)
        } else {
            binding.backgroundImg.setImageResource(R.color.black)
        }
    }
}