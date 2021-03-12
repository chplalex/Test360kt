package com.chplalex.Test360kt.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import com.bumptech.glide.Glide
import com.google.vr.cardboard.ThreadUtils.runOnUiThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

const val TAG = "Test360_debug"

fun String.fileName() = this.substring(this.lastIndexOf('/') + 1, this.length)

interface IImageLoaderCallBack {
    fun onSuccess(path: String)
    fun onFailure()
}

fun loadImage(context: Context, url: String, callBack: IImageLoaderCallBack) = CoroutineScope(Dispatchers.IO).launch {
    val result = runCatching {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .submit()
            .get()
    }
    if (result.isSuccess) {
        Log.d(TAG, "result is success")
        result.getOrNull()?.let {
            val path = saveImage(context, url, it)
            if (path.isEmpty()) {
                runOnUiThread { callBack.onFailure() }
            } else {
                runOnUiThread { callBack.onSuccess(path) }
            }
        }
    }
    if (result.isFailure) {
        runOnUiThread { callBack.onFailure() }
    }
}

private fun saveImage(context: Context, url: String, image: Bitmap): String {
    var imageAbsolutePath = ""
    val storageDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
                + "/pano360"
    )
    var success = true
    if (!storageDir.exists()) {
        success = storageDir.mkdirs()
    }
    if (success) {
        val imageFileName = url.fileName()
        val imageFile = File(storageDir, imageFileName)
        try {
            val fOut: OutputStream = FileOutputStream(imageFile)
            image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
            fOut.close()
            imageAbsolutePath = imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return imageAbsolutePath
}
