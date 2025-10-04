package com.haodustudio.DailyNotes.view.activities

import android.annotation.SuppressLint
import android.app.backup.BackupManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityMainBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.helper.toGson
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V1
import com.haodustudio.DailyNotes.model.database.NOTE_TYPE_V2
import com.haodustudio.DailyNotes.model.models.Note
import com.haodustudio.DailyNotes.utils.BitmapUtils
import com.haodustudio.DailyNotes.utils.DateUtils
import com.haodustudio.DailyNotes.utils.FileUtils
import com.haodustudio.DailyNotes.utils.NoteUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.adapters.BaseNoteAdapter
import com.haodustudio.DailyNotes.view.customView.sticker.StickerSaveModel
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import java.io.FileInputStream
import java.io.ObjectInputStream

class MainActivity : BaseActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val appViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }
    private lateinit var notesAdapter: BaseNoteAdapter
    private val mNoteList = ArrayList<BaseNoteAdapter.NoteItem>()
    private var mWakeLock: PowerManager.WakeLock? = null
    private var isStartupProcessDone = false
    private var backgroundPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        acquireWakeLock()
        runStartupChecks()
        setupClickListeners()
    }

    private fun runStartupChecks() {
        lifecycleScope.launch {
            beforeStartupCheck().flowOn(Dispatchers.IO)
                .onCompletion {
                    withContext(Dispatchers.Main) {
                        startMainPage()
                    }
                }
                .collect { message ->
                    withContext(Dispatchers.Main) {
                        binding.startTips.text = message
                        if (message == "Done") {
                            makeToast(getString(R.string.update_complete))
                            startActivity(Intent(this@MainActivity, GuideActivity::class.java))
                            startActivity(Intent(this@MainActivity, CongratulationsActivity::class.java))
                        }
                    }
                }
        }
    }

    private fun setupClickListeners() {
        binding.menu.setOnClickListener {
            binding.mDrawerLayout.openDrawer(GravityCompat.END)
        }

        binding.write.setOnClickListener {
            startActivity(Intent(this, NoteChangeMood::class.java).apply {
                putExtra("firstWrite", true)
            })
            binding.mDrawerLayout.closeDrawer(GravityCompat.END)
        }

        binding.moodCalendar.setOnClickListener {
            checkNotesAndStartActivity(MoodCalendar::class.java)
        }

        binding.personalize.setOnClickListener {
            checkNotesAndStartActivity(BackgroundChooser::class.java)
        }

        binding.find.setOnClickListener {
            checkNotesAndStartActivity(SearchNote::class.java)
        }

        binding.guide.setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
            binding.mDrawerLayout.closeDrawer(GravityCompat.END)
        }

        binding.about.setOnClickListener {
            startActivity(Intent(this, AboutSoftware::class.java))
            binding.mDrawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    private fun checkNotesAndStartActivity(activityClass: Class<*>) {
        if (mNoteList.isEmpty()) {
            makeToast(getString(R.string.please_write_one_note_at_least))
        } else {
            startActivity(Intent(this, activityClass))
        }
        binding.mDrawerLayout.closeDrawer(GravityCompat.END)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun startMainPage() {
        releaseWakeLock()
        isStartupProcessDone = true

        binding.startTips.visibility = View.GONE
        binding.startBg.visibility = View.GONE

        initAdapter()
        observeViewModel()
    }

    private fun observeViewModel() {
        appViewModel.notesList.observe(this) { notes ->
            mNoteList.clear()
            notes.forEach { note -> mNoteList.add(BaseNoteAdapter.NoteItem(note)) }
            mNoteList.sortByDescending {
                DateUtils.formatYYMMDD(it.data.yy, it.data.mm, it.data.dd, DateUtils.FORMAT_YYYY_MM_DD)
            }
            notesAdapter.notifyDataSetChanged()
        }

        appViewModel.appBackgroundPath.observe(this) { path ->
            backgroundPath = path
            refreshBackground()
        }
    }

    private fun initAdapter() {
        if (::notesAdapter.isInitialized) return

        binding.listView.setEmptyView(binding.emptyViewLayout.root)

        notesAdapter = BaseNoteAdapter(this, mNoteList)
        binding.listView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notesAdapter
        }

        notesAdapter.setOnItemClickListener { _, pos ->
            val targetNote = mNoteList[pos].data
            val intent = when (targetNote.type) {
                NOTE_TYPE_V1 -> Intent(this, NoteViewer::class.java)
                NOTE_TYPE_V2 -> Intent(this, FreeMakeNote::class.java)
                else -> null
            }
            intent?.let {
                it.putExtra("noteId", targetNote.id)
                startActivity(it)
            }
        }

        notesAdapter.setOnItemLongClickListener { _, pos ->
            val targetNote = mNoteList[pos].data
            startActivity(Intent(this, NoteOption::class.java).apply {
                putExtra("noteId", targetNote.id)
                putExtra("type", targetNote.type)
            })
            true
        }

        notesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                refreshBackground()
            }
        })
    }

    private suspend fun beforeStartupCheck() = flow {
        val sharedPreferences = getSharedPreferences(BaseApplication.APP_SHARED_PREFERENCES_NAME, 0)
        val transportCompleted = sharedPreferences.getBoolean("transportSuccessful", false)
        if (!transportCompleted) {
            emit(getString(R.string.updating_please_wait))
            delay(1500)

            FileUtils.delete(BaseApplication.OLD_ASSETS_PATH)
            FileUtils.makeRootDirectory(BaseApplication.NOTES_PATH)
            val oldNotes = NoteUtils.getOldNotesFileList()

            oldNotes.forEach { noteName ->
                val oldDir = "${BaseApplication.OLD_DATA_PATH}$noteName/"
                val newDir = "${BaseApplication.NOTES_PATH}$noteName/"
                try {
                    val date = DateUtils.getCalendarFromFormat(noteName, DateUtils.FORMAT_YYYY_MM_DD)
                    val moodInfo = FileUtils.readTxtFile(oldDir + "mood.txt").split("$[%|!|%]$")
                    val moodText = if (moodInfo.size > 1) moodInfo[1] else BaseApplication.code2MoodText_old[moodInfo[0].toInt()] ?: ""

                    val note = Note(
                        yy = DateUtils.getYYYYFromCalendar(date),
                        mm = DateUtils.getMMFromCalendar(date),
                        dd = DateUtils.getDDFromCalendar(date),
                        mood = Pair(moodInfo[0].toInt(), moodText),
                        type = NOTE_TYPE_V1,
                        data = hashMapOf(
                            "template" to Pair(false, FileUtils.readTxtFile(oldDir + "template.data").ifEmpty { "1" }).toGson(),
                            "textColor" to FileUtils.readTxtFile(oldDir + "textcolor.data").ifEmpty { "1" },
                            "noteFolder" to newDir,
                            "body" to FileUtils.readTxtFile(oldDir + "text.txt"),
                            "recordPaths" to FileUtils.getFilesList(oldDir + "record").map { "$newDir/record/$it" }.toGson(),
                            "imagePaths" to FileUtils.getFilesList(oldDir + "image").map { "$newDir/image/$it" }.toGson(),
                            "videoPaths" to FileUtils.getFilesList(oldDir + "video").map { "$newDir/video/$it" }.toGson(),
                        )
                    )
                    appViewModel.addNote(note)

                    FileUtils.copyFolder(oldDir + "record", newDir + "record")
                    FileUtils.copyFolder(oldDir + "image", newDir + "image")
                    FileUtils.copyFolder(oldDir + "video", newDir + "video")

                    if (FileUtils.exists(oldDir + "sitcker.sk")) {
                        convertStickerData(oldDir + "sitcker.sk", newDir)
                    }

                    emit("Moved $noteName Successful")
                    FileUtils.delete(oldDir)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit("Moved $noteName Failure")
                }
            }
            sharedPreferences.edit { putBoolean("transportSuccessful", true) }
            delay(200)
            emit("Done")
        }
    }

    private fun convertStickerData(oldPath: String, newDir: String) {
        ObjectInputStream(FileInputStream(oldPath)).use { ois ->
            val allPosData = ois.readObject() as ArrayList<FloatArray>
            val allBitmapData = ois.readObject() as ArrayList<ByteArray>
            val stickerPairs = allPosData.zip(allBitmapData).map { (pos, bytes) ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Pair(BitmapUtils.bitmapToString(bitmap), pos).also { bitmap.recycle() }
            }
            val stickerModel = StickerSaveModel(ArrayList(stickerPairs))
            FileUtils.writeTxtToFile(stickerModel.toGson(), newDir, "sticker.json")
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return if (isStartupProcessDone) super.dispatchTouchEvent(ev) else false
    }

    private fun refreshBackground() {
        if (backgroundPath.isNotEmpty() && mNoteList.isNotEmpty()) {
            Glide.with(this).load(backgroundPath).into(binding.backgroundImg)
        } else {
            binding.backgroundImg.setImageResource(android.R.color.black)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (mWakeLock?.isHeld != true) {
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "DailyNote:WakeLockTag")
                mWakeLock?.acquire(2 * 60 * 1000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (mWakeLock?.isHeld == true) {
                mWakeLock?.release()
                mWakeLock = null
            }
        } catch (_: Exception) {
        }
    }
}