package com.chplalex.Test360kt.galleries

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.utils.TAG
import com.dermandar.viewerlibrary.DMDViewer
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.lyrebirdstudio.fileboxlib.core.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class DMDViewerActivity : AppCompatActivity() {

    private lateinit var panoTitle: String
    private lateinit var panoUrl: String
    private lateinit var panoPath: String
    private lateinit var layout: RelativeLayout
    private lateinit var indicator: LinearProgressIndicator

    private var fileBox: FileBox? = null

    private val fileBoxCallBacks = fun (fileBoxResponse: FileBoxResponse) {
        when (fileBoxResponse) {
            is FileBoxResponse.Complete -> {
                panoPath = fileBoxResponse.record.getReadableFilePath()!!
                Log.d(TAG, "loading complete. panoPath = $panoPath")
                indicator.visibility = INVISIBLE
                loadPanoByPath()
            }
            is FileBoxResponse.Error -> {
                showToast("Pano loading error = ${fileBoxResponse.throwable}")
                Log.d(TAG, "Pano loading error = ${fileBoxResponse.throwable}")
                indicator.visibility = INVISIBLE
            }
        }
    }

    private var disposable:  Disposable? = null

    private val dmdViewer = DMDViewer()

    private val dmdCallBacks = object : DMDViewer.DMDViewerCallBacks {
        override fun onFinishLoading(suсcess: Boolean) {
            if (suсcess) {
                showToast("DMDViewer: Successful loading")
            } else {
                showToast("DMDViewer: Unsuccessful loading")
            }
        }

        override fun onReleased() {}

        override fun afterPanoLoad() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dmd_viewer)

        initViews()

        if (checkArguments()) {
            loadPanoByUrl()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileBox?.destroy()
        disposable?.dispose()
    }

    override fun onPause() {
        super.onPause()
        dmdViewer.pause()
    }

    override fun onResume() {
        super.onResume()
        dmdViewer.resume()
    }

    private fun checkArguments(): Boolean {
        val sourceData = intent.getParcelableExtra<SourceData>(DMD_VIEWER_ACTIVITY_KEY)

        if (sourceData == null) {
            showToast("Has no arguments")
            return false
        }

        if (sourceData.url.isNullOrEmpty()) {
            showToast("Arguments have no pano url")
            return false
        }

        panoTitle = sourceData.title ?: "Pano has no title"
        panoUrl = sourceData.url

        return true
    }

    private fun initViews() {
        layout = findViewById(R.id.layout_dmd_viewer)
        indicator = findViewById(R.id.indicator)
    }

    private fun loadPanoByUrl() {
        val fileBoxRequest = FileBoxRequest(panoUrl)
        fileBox = FileBoxProvider.newInstance(this, FileBoxConfig.createDefault())
        indicator.visibility = VISIBLE
        disposable = fileBox!!.get(fileBoxRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(fileBoxCallBacks)
    }

    private fun loadPanoByPath(): Boolean {
        var view: View? = null

        try {
            view = dmdViewer.init(this, panoPath, 90, "s", dmdCallBacks)
        } catch (exception: Exception) {
            Log.d(TAG, "DMDViewer init() exception = $exception")
            Toast.makeText(this, "DMDViewer init() exception = $exception", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            layout.addView(view)
        } catch (exception: Exception) {
            Log.d(TAG, "layout DMDViewer add() exception = $exception")
            Toast.makeText(this, "layout DMDViewer add() exception = $exception", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showToast(msg: String) = runOnUiThread {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val DMD_VIEWER_ACTIVITY_KEY = DMDViewerActivity::class.java.name + "_KEY"

        fun start(context: Context, sourceData: SourceData) =
            Intent(context, DMDViewerActivity::class.java).apply {
                putExtra(DMD_VIEWER_ACTIVITY_KEY, sourceData)
                context.startActivity(this)
            }
    }
}