package com.haodustudio.DailyNotes.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

object PlayMediaUtils {
    private const val TAG = "PlayMediaUtils"

    private val mp: MediaPlayer = MediaPlayer().apply {
        applyDefaultAudioAttributes()
    }

    private fun MediaPlayer.applyDefaultAudioAttributes() {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }

    @JvmStatic
    fun play(path: String, onError: ((Throwable) -> Unit)? = null) {
        try {
            initMP(path)
            if (!mp.isPlaying) {
                mp.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start media from $path", e)
            onError?.invoke(e)
        }
    }
    fun pause(){
        if(mp.isPlaying)
            mp.pause()
    }

    fun stop(){
        try {
            mp.stop()
            mp.reset()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    @Throws(IOException::class, IllegalStateException::class, SecurityException::class)
    fun initMP(path: String) {
        mp.reset()
        mp.applyDefaultAudioAttributes()
        mp.setDataSource(path)
        mp.prepare()
    }

    fun isPlaying():Boolean {
        return try {
            mp.isPlaying
        }catch (e:Exception){
            e.printStackTrace()
            false
        }
    }

    fun clean(){
        mp.release()
    }

    fun getTime(): Int{
        return try {
            mp.duration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read duration", e)
            0
        }
    }

    fun getPos(): Int = try {
        mp.currentPosition
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read current position", e)
        0
    }

    fun setOnCompleteListener(func : () -> Unit) {
        mp.setOnCompletionListener {
            func()
        }
    }

    fun setOnMediaBeReadyListener(func : () -> Unit) {
        mp.setOnPreparedListener {
            func()
        }
    }
}