package com.chplalex.Test360kt.galleries

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.utils.TAG
import com.dermandar.viewerlibrary.DMDViewer

class DMDViewerActivity: AppCompatActivity() {

    private val dmdViewer = DMDViewer()

    companion object {
        private val DMD_VIEWER_ACTIVITY_KEY = DMDViewerActivity::class.java.name + "_KEY"

        fun start(context: Context, sourceData: SourceData) =
            Intent(context, DMDViewerActivity::class.java).apply {
                putExtra(DMD_VIEWER_ACTIVITY_KEY, sourceData)
                context.startActivity(this)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dmd_viewer)

        val layout = findViewById<RelativeLayout>(R.id.layout_dmd_viewer)
        val sourceData = intent.getParcelableExtra<SourceData>(DMD_VIEWER_ACTIVITY_KEY)
        sourceData?.let{
            supportActionBar?.title = it.title
            try {
                val view = dmdViewer.init(this, it.url, 150, "c", null)
                layout.addView(view)
            } catch(exception: Exception) {
                Log.d(TAG, "exception = $exception")
                Toast.makeText(this, "Error = $exception", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dmdViewer.pause()
    }

    override fun onResume() {
        super.onResume()
        dmdViewer.resume()
    }
}