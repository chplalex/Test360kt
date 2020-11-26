package com.chplalex.Test360kt.cameras

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.*
import com.chplalex.Test360kt.cameras.PanoShooterActivity.Companion.PANO_SHOOTER_RESULT_URI_KEY
import com.chplalex.Test360kt.cameras.save.ShooterActivity_save
import com.chplalex.Test360kt.galleries.PanoramaActivity
import com.chplalex.Test360kt.galleries.SourceData
import com.chplalex.Test360kt.utils.TAG

private const val CAMERA_REQUEST = 4747

class CamerasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_cameras,
        container,
        false
    )?.also { it ->

        activity?.title = resources.getString(R.string.label_cameras)

        it.findViewById<ImageButton>(R.id.imgCamSimple)?.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
                startActivityForResult(it, CAMERA_REQUEST)
            }
        }

        it.findViewById<ImageButton>(R.id.imgCamSimpleRight)?.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
                startActivityForResult(it, CAMERA_REQUEST)
            }
        }

        it.findViewById<ImageButton>(R.id.imgCamFull)?.setOnClickListener {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also {
                startActivityForResult(it, CAMERA_REQUEST)
            }
        }

        it.findViewById<ImageButton>(R.id.imgCamFullRight)?.setOnClickListener {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also {
                startActivityForResult(it, CAMERA_REQUEST)
            }
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDKSave)?.setOnClickListener {
            ShooterActivity_save.start(requireContext())
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDKSaveRight)?.setOnClickListener {
            ShooterActivity_save.start(requireContext())
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDK)?.setOnClickListener {
            //val panoFileName = getNewPanoFileName()
            //PanoShooterActivity.start(it.context, panoFileName)
            // точка входа в основном приложении
            activity?.let { itActivity -> openCameraPano(itActivity) }
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDKRight)?.setOnClickListener {
            activity?.let { itActivity -> openCameraPano(itActivity) }
        }

    }

    private fun openCameraPano(activity: FragmentActivity) {
        val takePictureIntent = Intent(activity, PanoShooterActivity::class.java)
        val file = createImageFile(requireContext())
        val uri = Uri.fromFile(file)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult(), requestCode = $requestCode, resultCode = $resultCode, data = $data")

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            Toast.makeText(context, "Отображение результата (заглушка)", Toast.LENGTH_LONG).show()
        }

        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            data?.apply {
                val panoramaUri: Uri? = getParcelableExtra(PANO_SHOOTER_RESULT_URI_KEY)
                panoramaUri?.let { uri ->
                    Log.d(TAG, "Панорама сохранена. Path = ${uri.path}")
                    context?.let { PanoramaActivity.start(it, SourceData("Свежее фото", uri.path)) }
                }
            }
        }
    }
}