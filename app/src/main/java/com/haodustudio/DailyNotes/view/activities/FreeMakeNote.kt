package com.haodustudio.DailyNotes.view.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityFreeMakeNoteBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.helper.toGson
import com.haodustudio.DailyNotes.model.listener.ChangeNoteDataCallBack
import com.haodustudio.DailyNotes.model.models.Note
import com.haodustudio.DailyNotes.utils.BitmapFilletUtils
import com.haodustudio.DailyNotes.utils.BitmapUtils
import com.haodustudio.DailyNotes.utils.DisplayUtil
import com.haodustudio.DailyNotes.utils.FileUtils
import com.haodustudio.DailyNotes.view.activities.base.DialogActivity
import com.haodustudio.DailyNotes.view.customView.freeLayout.ObjectSaveModel
import com.haodustudio.DailyNotes.view.customView.freeLayout.objects.BitmapObject
import com.haodustudio.DailyNotes.view.customView.freeLayout.objects.TextObject
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CheckResult")
class FreeMakeNote : DialogActivity(noShot = true, canDis = true) {

    private val binding by lazy { ActivityFreeMakeNoteBinding.inflate(layoutInflater) }
    private val appViewModel: GlobalViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }

    private var note: Note? = null
    private var noteId: Long = -1L
    private var isEditMode: Boolean = false
    private var currentBackgroundColorId = 12

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.extras?.getString(MediaStore.EXTRA_OUTPUT)?.let { photoPath ->
                binding.freeLayout.addFreeObj(BitmapObject(BitmapUtils.getImageFromPath(photoPath)))
                binding.addSomethingRoot.isVisible = false
            } ?: makeToast("Failed to get image path")
        }
    }

    private val stickerPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val data = result.data ?: return@registerForActivityResult
                val bitmapPath = data.getStringExtra("bitmapPath")
                val stickerPath = data.getStringExtra("stickerPath")

                val bitmap = when {
                    !bitmapPath.isNullOrEmpty() -> BitmapUtils.getImageFromPath(bitmapPath)
                    !stickerPath.isNullOrEmpty() -> BitmapUtils.getImageFromAssetsFile(stickerPath)
                    else -> null
                }

                bitmap?.let {
                    binding.freeLayout.addFreeObj(BitmapObject(it))
                    binding.addSomethingRoot.isVisible = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                makeToast("添加失败")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        handleIntentExtras()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupUI()
        setupObservers()
    }

    private fun handleIntentExtras() {
        noteId = intent.getLongExtra("noteId", -1L)
        isEditMode = intent.getBooleanExtra("editMode", false)

        setTheme(if (isEditMode) R.style.DialogActivityTheme else R.style.DialogActivityTheme2)

        if (noteId == -1L) {
            makeToast("Empty id")
            finish()
            return
        }
    }

    private fun setupUI() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        updateUiForEditMode()
        setupClickListeners()
        setupSeekBarListeners()
    }

    private fun setupObservers() {
        appViewModel.getNoteFromIdLiveData(noteId).observe(this) { newNote ->
            newNote ?: return@observe
            val isFirstLoad = this.note == null
            this.note = newNote
            if (isFirstLoad) {
                loadNoteContent(newNote)
            }
        }
    }

    private fun loadNoteContent(noteToLoad: Note) {
        try {
            loadFreeObjects(noteToLoad.data["noteFolder"])
            binding.freeLayout.addPaper(noteToLoad.data["pageSize"]?.toInt() ?: 0)
            currentBackgroundColorId = noteToLoad.data["backgroundColor"]?.toInt() ?: 12
            val colorRes = BaseApplication.idToTextColor[currentBackgroundColorId] ?: R.color.white
            binding.backgroundColorRoot.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        } catch (e: Exception) {
            e.printStackTrace()
            makeToast("加载失败")
        }
    }

    private fun updateUiForEditMode() {
        binding.freeLayout.editMode = isEditMode
        binding.addPaper.isVisible = isEditMode
        binding.removePaper.isVisible = isEditMode
        binding.showRight.isVisible = isEditMode
    }

    private fun setupClickListeners() {
        binding.backToEdit.setOnClickListener { finish() }
        binding.complete.setOnClickListener { saveNoteAndRecreate() }
        binding.showRight.setOnClickListener { openRightDrawer() }

        binding.freeLayout.setOnObjFocusListener { _, isFocus ->
            binding.objPopView.isVisible = isFocus
        }

        binding.objAngleLeft.setOnClickListener { binding.freeLayout.getFocusObj()?.rotate(-10f) }
        binding.objAngleRight.setOnClickListener { binding.freeLayout.getFocusObj()?.rotate(10f) }
        binding.delObj.setOnClickListener {
            binding.freeLayout.getFocusObj()?.let { binding.freeLayout.deleteFreeObj(it) }
            binding.objPopView.isVisible = false
        }

        binding.addObj.setOnClickListener {
            binding.addSomethingRoot.isVisible = true
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
        binding.addBack.setOnClickListener { binding.addSomethingRoot.isVisible = false }
        binding.addImage.setOnClickListener { launchImagePicker() }
        binding.addSticker.setOnClickListener { stickerPickerLauncher.launch(Intent(this, NoteStickerChooser::class.java)) }
        binding.addText.setOnClickListener {
            binding.addSomethingRoot.isVisible = false
            binding.addTextAction.isVisible = true
        }
        binding.addBackground.setOnClickListener { showColorBoard(isForBackground = true) }

        binding.addPaper.setOnClickListener { binding.freeLayout.addPaper() }
        binding.removePaper.setOnClickListener { binding.freeLayout.deletePaper() }

        setupTextPanelListeners()
    }

    private fun setupTextPanelListeners() {
        binding.cancelAddText.setOnClickListener { binding.addTextAction.isVisible = false }
        binding.completeAddText.setOnClickListener { addTextObjectFromInput() }

        binding.textStyleReturn.setOnClickListener {
            binding.addTextEdit.text?.insert(binding.addTextEdit.selectionStart, "\n")
        }
        binding.textStyleBold.setOnClickListener {
            it.isSelected = !it.isSelected
            binding.addTextEdit.paint.isFakeBoldText = it.isSelected
        }
        binding.textStyleItalic.setOnClickListener {
            it.isSelected = !it.isSelected
            binding.addTextEdit.paint.textSkewX = if (it.isSelected) -0.2f else 0f
        }
        binding.textStyleUnderline.setOnClickListener {
            it.isSelected = !it.isSelected
            binding.addTextEdit.paint.isUnderlineText = it.isSelected
        }
        binding.textStyleStrikethrough.setOnClickListener {
            it.isSelected = !it.isSelected
            binding.addTextEdit.paint.isStrikeThruText = it.isSelected
        }
        binding.textStyleChangeColor.setOnClickListener { showColorBoard(isForBackground = false) }
    }

    private fun addTextObjectFromInput() {
        val text = binding.addTextEdit.text.toString()
        if (text.isNotEmpty()) {
            binding.freeLayout.addFreeObj(TextObject(text, binding.addTextEdit.paint))
            resetTextCreationPanel()
        } else {
            makeToast("文字不能为空")
        }
    }

    private fun resetTextCreationPanel() {
        binding.addTextAction.isVisible = false
        binding.addTextEdit.text.clear()
        binding.addTextEdit.paint.apply {
            isFakeBoldText = false
            textSkewX = 0f
            isUnderlineText = false
            isStrikeThruText = false
            color = ContextCompat.getColor(this@FreeMakeNote, R.color.black)
        }
        binding.textStyleBold.isSelected = false
        binding.textStyleItalic.isSelected = false
        binding.textStyleUnderline.isSelected = false
        binding.textStyleStrikethrough.isSelected = false
    }

    private fun setupSeekBarListeners() {
        binding.objSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                binding.freeLayout.getFocusObj()?.let { obj ->
                    val values = FloatArray(9).apply { obj.mMatrix.getValues(this) }
                    val currentWidth = obj.srcBitmap.width * values[0]
                    if (currentWidth > 0) {
                        val scaleValue = (progress.toFloat() + 50) / currentWidth
                        obj.scale(scaleValue, scaleValue)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun showColorBoard(isForBackground: Boolean) {
        binding.colorsBoard.isVisible = true
        (binding.colorsBoard.getChildAt(0) as ViewGroup).children.forEachIndexed { index, view ->
            view.setOnClickListener {
                val colorId = index + 1
                val color = ContextCompat.getColor(this, BaseApplication.idToTextColor[colorId]!!)
                if (isForBackground) {
                    binding.backgroundColorRoot.setBackgroundColor(color)
                    currentBackgroundColorId = colorId
                    binding.addSomethingRoot.isVisible = false
                } else {
                    binding.addTextEdit.setTextColor(color)
                    binding.addTextEdit.paint.color = color
                }
                binding.colorsBoard.isVisible = false
            }
        }
    }

    private fun saveNoteAndRecreate() {
        val currentNote = note ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dirPath = currentNote.data["noteFolder"]!!
                val objSaveList = binding.freeLayout.getAllObj().map { obj ->
                    val matrixValues = FloatArray(9).apply { obj.mMatrix.getValues(this) }
                    Pair(BitmapUtils.bitmapToString(obj.srcBitmap), matrixValues)
                }
                val saveModel = ObjectSaveModel(ArrayList(objSaveList))
                FileUtils.writeTxtToFile(saveModel.toGson(), dirPath, "free_obj.json")

                appViewModel.changeNoteDataFromId(noteId, object : ChangeNoteDataCallBack {
                    override fun doChange(it: Note) {
                        it.data["pageSize"] = binding.freeLayout.getPaperSize().toString()
                        it.data["backgroundColor"] = currentBackgroundColorId.toString()
                    }

                    override fun onChangeSuccessful() {
                        intent.putExtra("editMode", false)
                        recreate()
                    }

                    override fun onChangeFailure(e: Exception) {
                        e.printStackTrace()
                        makeToast("保存失败")
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { makeToast("保存失败") }
            }
        }
    }

    private fun loadFreeObjects(dirPath: String?) {
        dirPath ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val filePath = "$dirPath/free_obj.json"
            if (!FileUtils.exists(filePath)) {
                showEmptyNoteToast(); return@launch
            }

            val content = FileUtils.readTxtFile(filePath)
            if (content.isNullOrBlank()) {
                showEmptyNoteToast(); return@launch
            }

            val model = Gson().fromJson(content, ObjectSaveModel::class.java)
            withContext(Dispatchers.Main) {
                binding.freeLayout.clear()
                if (model.objList.isEmpty()) {
                    showEmptyNoteToast()
                } else {
                    model.objList.forEach { objData ->
                        BitmapObject(BitmapUtils.stringToBitmap(objData.first)).apply {
                            mMatrix.setValues(objData.second)
                            updatePoints()
                            binding.freeLayout.addFreeObj(this)
                        }
                    }
                }
            }
        }
    }

    private suspend fun showEmptyNoteToast() {
        if (!isEditMode) {
            withContext(Dispatchers.Main) { makeToast("这篇手帐是空的") }
        }
    }

    private fun launchImagePicker() {
        PermissionX.init(this)
            .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                        putExtra("com.xtc.camera.LEFT_BUTTON_TEXT", "关闭")
                        putExtra("com.xtc.camera.RIGHT_BUTTON_TEXT", "选择")
                    }
                    imagePickerLauncher.launch(intent)
                } else {
                    makeToast("Permission denied")
                }
            }
    }

    private fun openRightDrawer() {
        try {
            val viewBitmap = BitmapUtils.viewConversionBitmap(binding.drawerLayout)
            val blurredBitmap = BitmapUtils.rsBlur(viewBitmap, 8)
            val croppedBitmap = Bitmap.createBitmap(
                blurredBitmap,
                binding.drawerLayout.width - DisplayUtil.dip2px(55f), 0,
                DisplayUtil.dip2px(55f), binding.drawerLayout.height
            )
            val finalBitmap = BitmapFilletUtils.fillet(
                croppedBitmap, DisplayUtil.dip2px(15f), BitmapFilletUtils.CORNER_LEFT
            )

            binding.popbk.setImageBitmap(finalBitmap)
            viewBitmap.recycle()
            blurredBitmap.recycle()

            binding.drawerLayout.openDrawer(GravityCompat.END)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}