package com.haodustudio.DailyNotes.view.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.databinding.ActivityNoteViewerBinding
import com.haodustudio.DailyNotes.helper.*
import com.haodustudio.DailyNotes.model.listener.ChangeNoteDataCallBack
import com.haodustudio.DailyNotes.model.models.Note
import com.haodustudio.DailyNotes.utils.BitmapUtils
import com.haodustudio.DailyNotes.utils.DateUtils
import com.haodustudio.DailyNotes.utils.FileUtils
import com.haodustudio.DailyNotes.utils.NoteUtils
import com.haodustudio.DailyNotes.utils.ninePatch.NinePatchChunk
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.adapters.FixLinearLayoutManager
import com.haodustudio.DailyNotes.view.adapters.ImageAdapter
import com.haodustudio.DailyNotes.view.adapters.RecordAdapter
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NoteViewer : BaseActivity() {

    private val binding by lazy { ActivityNoteViewerBinding.inflate(layoutInflater) }
    private val appViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }
    private lateinit var noteData: Note

    private lateinit var imageAdapter: ImageAdapter
    private val imageList = ArrayList<ImageAdapter.ImageItem>()
    private lateinit var recordAdapter: RecordAdapter
    private val recordList = ArrayList<RecordAdapter.RecordItem>()

    private var isEditing: Boolean = false
        set(value) {
            field = value
            binding.editModeControls.visibility = if (value) View.VISIBLE else View.GONE
            binding.stickerLayout.visibility = if (value) View.GONE else View.VISIBLE
        }

    private val addStickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("NoteViewer", "Reload Sticker")
            binding.stickerLayout.visibility = View.VISIBLE
            loadSticker()
            binding.stickerLayout.cleanAllFocus()
        } else {
            isEditing = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.stickerLayout.setCanEdit(false)

        val noteId = intent.getLongExtra("noteId", -1L)
        isEditing = intent.getBooleanExtra("editMode", false)

        if (noteId == -1L) {
            makeToast("Empty id")
            finish()
            return
        }

        appViewModel.getNoteFromIdLiveData(noteId).observe(this) { updatedNote ->
            noteData = updatedNote
            updateUI(updatedNote)
        }

        setupListeners()
    }

    private fun updateUI(note: Note) {
        try {
            updateTittle(note.yy, note.mm, note.dd)
            updateMood(note.mood)
            updateBackground(note.data["template"]!!.toPair())
            updateTextColor(note.data["textColor"]!!)
            updateBodyText(note.data["body"]!!)
            updateImage(note.data["imagePaths"]!!.toArray(), note.data["videoPaths"]!!.toArray())
            updateRecord(note.data["recordPaths"]!!.toArray())
            loadSticker(note)
        } catch (e: Exception) {
            makeToast("Failed to load note data")
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        binding.addMedia.setOnClickListener { navigateTo(NoteAddMedia::class.java) }
        binding.editBodyText.setOnClickListener { navigateTo(NoteEditBody::class.java) }
        binding.editMood.setOnClickListener { navigateTo(NoteChangeMood::class.java) }
        binding.editTemplate.setOnClickListener { navigateTo(NoteChangeTemplate::class.java) }
        binding.editStickersButton.setOnClickListener { launchStickerEditor() }
    }

    private fun navigateTo(activityClass: Class<*>) {
        if (!::noteData.isInitialized) return
        val intent = Intent(this, activityClass).putExtra("noteId", noteData.id)
        startActivity(intent)
    }

    private fun launchStickerEditor() {
        isEditing = false
        lifecycleScope.launch {
            delay(500)
            val bitmap = BitmapUtils.viewConversionBitmap(binding.viewForShot)
            FileUtils.saveBitmap(File(cacheDir, "tempNoteShot.jpg").absolutePath, bitmap)
            val intent = Intent(this@NoteViewer, NoteAddSticker::class.java)
            intent.putExtra("noteId", noteData.id)
            addStickerLauncher.launch(intent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecord(pathList: ArrayList<String>) {
        if (!::recordAdapter.isInitialized) {
            recordAdapter = RecordAdapter(this, recordList)
            binding.recordList.apply {
                layoutManager = FixLinearLayoutManager(this@NoteViewer)
                adapter = recordAdapter
            }
            recordAdapter.setOnItemClickListener { _, i ->
                startActivity(Intent(this, PlayRecord::class.java).putExtra("path", recordList[i].path))
            }
            recordAdapter.setOnItemLongClickListener { _, i ->
                if (isEditing) {
                    DeleteSafeCheck.check(this) { confirmed ->
                        if (confirmed) removeItemFromNote(recordList[i].path, "recordPaths")
                    }
                }
                true
            }
        }
        recordList.clear()
        pathList.forEach { recordList.add(RecordAdapter.RecordItem(it)) }
        recordList.sortBy { File(it.path).nameWithoutExtension.safeToLong() }
        recordAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateImage(imagePath: ArrayList<String>, videoPath: ArrayList<String>) {
        if (!::imageAdapter.isInitialized) {
            imageAdapter = ImageAdapter(this@NoteViewer, imageList)
            binding.imageList.apply {
                layoutManager = FixLinearLayoutManager(this@NoteViewer)
                adapter = imageAdapter
            }
            imageAdapter.setOnItemClickListener { _, i ->
                val item = imageList[i]
                startActivity(Intent(this, ViewImage::class.java).apply {
                    putExtra("isVideo", item.isVideo)
                    putExtra("path", item.path)
                })
            }
            imageAdapter.setOnItemLongClickListener { _, i ->
                if (isEditing) {
                    val item = imageList[i]
                    DeleteSafeCheck.check(this) { confirmed ->
                        if (confirmed) {
                            val key = if (item.isVideo) "videoPaths" else "imagePaths"
                            removeItemFromNote(item.path, key)
                        }
                    }
                }
                true
            }
        }
        imageList.clear()
        imagePath.sortedBy { it.safeToLong() }.mapTo(imageList) { ImageAdapter.ImageItem(it, false) }
        videoPath.sortedBy { it.safeToLong() }.mapTo(imageList) { ImageAdapter.ImageItem(it, true) }
        imageAdapter.notifyDataSetChanged()
    }

    private fun removeItemFromNote(path: String, propertyKey: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            appViewModel.changeNoteDataFromId(noteData.id, object : ChangeNoteDataCallBack {
                override fun doChange(it: Note) {
                    val updatedList = it.data[propertyKey]!!.toArray<String>().apply { remove(path) }
                    it.data[propertyKey] = updatedList.toGson()
                }
                override fun onChangeSuccessful() {
                    FileUtils.delete(path)
                }
                override fun onChangeFailure(e: Exception) {
                    e.printStackTrace()
                    lifecycleScope.launch(Dispatchers.Main) { makeToast("Deleted failure") }
                }
            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTittle(yy: Int, mm: Int, dd: Int) {
        binding.date.text = "$yy-$mm-$dd"
        binding.dayOfWeek.text = DateUtils.getDayOfWeek(yy, mm, dd)
    }

    private fun updateMood(mood: Pair<Int, String>) {
        Glide.with(this)
            .load(BitmapUtils.getImageFromAssetsFile("${BaseApplication.ASSETS_MOOD_PATH}${mood.first}.png"))
            .into(binding.moodImage)
        binding.moodText.text = mood.second
    }

    private fun updateBackground(tmp: Pair<Boolean, String>) {
        val bitmap: Bitmap = if (tmp.first) {
            BitmapUtils.getImageFromPath(tmp.second)
        } else {
            BitmapUtils.getImageFromAssetsFile("${BaseApplication.ASSETS_TEMPLATE_PATH}${tmp.second}.9.png")
        }
        binding.templateBackground.background = NinePatchChunk.create9PatchDrawable(this, bitmap, "background")
    }

    private fun updateTextColor(colorId: String) {
        BaseApplication.idToTextColor[colorId.toInt()]?.let {
            binding.bodyText.setTextColor(getColor(it))
        }
    }

    private fun updateBodyText(str: String) {
        binding.bodyText.setText(str)
    }

    private fun loadSticker(note: Note = noteData) {
        try {
            NoteUtils.loadSticker(binding.stickerLayout, note)
        } catch (e: Exception) {
            e.printStackTrace()
            makeToast("View sticker failure")
        }
    }

}