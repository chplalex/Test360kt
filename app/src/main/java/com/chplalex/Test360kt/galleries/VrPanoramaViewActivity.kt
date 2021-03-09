package com.chplalex.Test360kt.galleries

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.chplalex.Test360kt.R
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.google.vr.sdk.widgets.pano.VrPanoramaView.Options.TYPE_MONO
import kotlinx.android.synthetic.main.activity_vr_panorama_view.*

class VrPanoramaViewActivity : AppCompatActivity() {

    companion object {
        private val VR_PANORAMA_VIEW_ACTIVITY_KEY = VrPanoramaViewActivity::class.java.name + "_KEY"

        fun start(context: Context, sourceData: SourceData) =
            Intent(context, VrPanoramaViewActivity::class.java).apply {
                putExtra(VR_PANORAMA_VIEW_ACTIVITY_KEY, sourceData)
                context.startActivity(this)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vr_panorama_view)

        scrollingByGesture.setOnCheckedChangeListener { buttonView, isChecked -> vrPanoramaView.setPureTouchTracking(isChecked) }
        scrollingByGiro.setOnCheckedChangeListener { buttonView, isChecked -> vrPanoramaView.setPureTouchTracking(!isChecked) }

        val sourceData = intent.getParcelableExtra<SourceData>(VR_PANORAMA_VIEW_ACTIVITY_KEY)
        if (sourceData != null) {
            supportActionBar?.title = sourceData.title
            val options = VrPanoramaView.Options().also { it.inputType = TYPE_MONO }
            Glide
                .with(this)
                .asBitmap()
                .load(sourceData.url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        vrPanoramaView.loadImageFromBitmap(resource, options)
                        vrPanoramaView.setPureTouchTracking(true)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {  }
                })
        }
    }

    override fun onPause() {
        super.onPause()
        vrPanoramaView.pauseRendering()
    }

    override fun onResume() {
        super.onResume()
        vrPanoramaView.resumeRendering()
    }
}