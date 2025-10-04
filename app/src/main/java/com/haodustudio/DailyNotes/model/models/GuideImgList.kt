package com.haodustudio.DailyNotes.model.models

import com.google.gson.annotations.SerializedName

data class GuideImgList (
    @SerializedName("guide_img_list")
    val imgList: List<String>,

    @SerializedName("result_code")
    val code: Int?,
) {
    fun getList(): List<String> {
        if (code != 0) {
            throw RuntimeException("Result code is 1 or null")
        }
        return imgList
    }
}