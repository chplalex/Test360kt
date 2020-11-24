package com.chplalex.Test360kt

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat

const val REQUEST_CODE_CAMERA = 783
var tempImageFilePath: String? = null

@SuppressLint("SimpleDateFormat")
fun createImageFile(context: Context): File {
    val TEMP_IMAGE_DATE_FORMAT = "yyyyMMdd_HHmmss"
    val timeStamp = SimpleDateFormat(TEMP_IMAGE_DATE_FORMAT).format(System.currentTimeMillis())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    //return File.createTempFile(imageFileName, ".jpg", storageDir)
    return File("$storageDir/$imageFileName.jpg")
}

fun getUriForFile(context: Context, file: File): Uri {
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}

@SuppressLint("QueryPermissionsNeeded")
fun grantUriPermission(context: Context, intent: Intent, uri: Uri) {
    val resolvedIntentActivities =
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    resolvedIntentActivities.forEach { info ->
        val packageName = info.activityInfo.packageName
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}



