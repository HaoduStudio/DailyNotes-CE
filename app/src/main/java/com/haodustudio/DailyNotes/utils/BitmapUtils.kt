@file:Suppress("DEPRECATION")

package com.haodustudio.DailyNotes.utils

import android.app.Activity
import android.content.res.AssetManager
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Base64
import android.util.Log
import android.view.View
import com.haodustudio.DailyNotes.BaseApplication
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream


object BitmapUtils {
    
    /**
     * 安全地从资源中加载图片,自动计算采样率避免 OOM 和 Canvas 绘制过大 Bitmap 错误
     * @param resId 图片资源ID
     * @param reqWidth 需要的宽度(如果为0则不限制)
     * @param reqHeight 需要的高度(如果为0则不限制)
     * @return 采样后的 Bitmap,如果加载失败返回 null
     */
    fun decodeSampledBitmapFromResource(resId: Int, reqWidth: Int = 0, reqHeight: Int = 0): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(BaseApplication.instance.resources, resId, options)

            // 计算采样率
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // 实际解码图片
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // 使用更少内存
            
            BitmapFactory.decodeResource(BaseApplication.instance.resources, resId, options)
        } catch (e: Exception) {
            Log.e("BitmapUtils", "Failed to decode bitmap from resource: $resId", e)
            null
        }
    }

    /**
     * 计算合适的采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        // 如果没有指定需求尺寸,使用屏幕尺寸作为限制
        val finalReqWidth = if (reqWidth <= 0) {
            BaseApplication.instance.resources.displayMetrics.widthPixels
        } else reqWidth
        
        val finalReqHeight = if (reqHeight <= 0) {
            BaseApplication.instance.resources.displayMetrics.heightPixels
        } else reqHeight

        if (height > finalReqHeight || width > finalReqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // 计算最大的 inSampleSize 值,保证采样后的图片尺寸大于需求尺寸
            while (halfHeight / inSampleSize >= finalReqHeight && 
                   halfWidth / inSampleSize >= finalReqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    
    fun getImageFromAssetsFile(filePath: String): Bitmap {
        val am: AssetManager = BaseApplication.instance.resources.assets
        val mIs: InputStream = am.open(filePath)
        val image = BitmapFactory.decodeStream(mIs)
        mIs.close()
        return image
    }

    fun getImageFromPath(path: String): Bitmap {
        val fis = FileInputStream(path)
        return BitmapFactory.decodeStream(fis)
    }

    fun getActivityShotBitmap(activity: Activity): Bitmap {
        val view: View = activity.window.decorView
        return viewConversionBitmap(view)
    }

    // 高斯模糊
    fun rsBlur(source: Bitmap, radius: Int = 0): Bitmap {
        val renderScript = RenderScript.create(BaseApplication.instance)
        Log.i("BitmapUtils", "scale size:" + source.width + "*" + source.height)
        val input = Allocation.createFromBitmap(renderScript, source)
        val output = Allocation.createTyped(renderScript, input.type)
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        scriptIntrinsicBlur.setInput(input)
        if (radius != 0) {
            scriptIntrinsicBlur.setRadius(radius.toFloat())
        }
        scriptIntrinsicBlur.forEach(output)
        output.copyTo(source)
        renderScript.destroy()
        return source
    }

    fun viewConversionBitmap(v: View, config:Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val w = v.width
        val h = v.height
        val bmp = Bitmap.createBitmap(w, h, config)
        val c = Canvas(bmp)
        /** 如果不设置canvas画布为白色，则生成透明  */
        v.layout(0, 0, w, h)
        v.draw(c)
        return bmp
    }

    fun stringToBitmap(string: String): Bitmap {
        // 将字符串转换成Bitmap类型
        val bitmap: Bitmap
        val bitmapArray: ByteArray = Base64.decode(string, Base64.DEFAULT)
        bitmap = BitmapFactory.decodeByteArray(
            bitmapArray, 0,
            bitmapArray.size
        )
        return bitmap
    }

    fun bitmapToString(bitmap: Bitmap): String {
        //将Bitmap转换成字符串
        var string: String? = null
        val bStream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, bStream)
        val bytes: ByteArray = bStream.toByteArray()
        string = Base64.encodeToString(bytes, Base64.DEFAULT)
        return string
    }
}