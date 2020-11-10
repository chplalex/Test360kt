package com.chplalex.Test360kt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class ShooterActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) =
            Intent(context, ShooterActivity::class.java).apply {
                context.startActivity(this)
            }
    }

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

    }

}