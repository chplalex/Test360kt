package com.chplalex.test360kt.cameras

import android.content.Context
import android.content.Intent
import android.graphics.Insets
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.test360kt.R
import com.chplalex.test360kt.utils.TAG


class ShooterActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) =
            Intent(context, ShooterActivity::class.java).apply {
                context.startActivity(this)
            }
    }

    private var mRelativeLayout: RelativeLayout? = null
    private var mWidth = 400
    private var mHeight = 500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN
//        )

        //TODO: здесь нужно проверить и при необходимости запросить различные разрешения

        onCreateSub();
    }

    private fun onCreateSub() {
        setContentView(R.layout.activity_shooter)

        mRelativeLayout = findViewById<View>(R.id.relativeLayout) as RelativeLayout

        val display = getDisplay()
        val displayMetrics = DisplayMetrics()
        display?.getMetrics(displayMetrics)

        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels

        startShooter()
    }

    private fun startShooter() {
        Log.d(TAG, "mWidth = $mWidth")
        Log.d(TAG, "mHeight = $mHeight")
    }

}