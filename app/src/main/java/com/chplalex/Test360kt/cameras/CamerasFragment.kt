package com.chplalex.Test360kt.cameras

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider.getUriForFile
import androidx.fragment.app.FragmentActivity
import com.chplalex.Test360kt.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat

private const val CAMERA_REQUEST = 4747

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CamerasFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CamerasFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
                startActivityForResult(
                    it,
                    CAMERA_REQUEST
                )
            }
        }

        it.findViewById<ImageButton>(R.id.imgCamSimpleRight)?.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
                startActivityForResult(
                    it,
                    CAMERA_REQUEST
                )
            }
        }

        it.findViewById<ImageButton>(R.id.imgCamFull)?.setOnClickListener {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also {
                startActivityForResult(
                    it,
                    CAMERA_REQUEST
                )
            }
        }

        it.findViewById<ImageButton>(R.id.imgCamFullRight)?.setOnClickListener {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also {
                startActivityForResult(
                    it,
                    CAMERA_REQUEST
                )
            }
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDK)?.setOnClickListener {
            //val panoFileName = getNewPanoFileName()
            //PanoShooterActivity.start(it.context, panoFileName)
            // точка входа в основном приложении
            openCameraPano(activity)
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDKRight)?.setOnClickListener {
            PanoShooterActivity.start(it.context)
        }

    }

    private fun openCameraPano(activity: FragmentActivity) {
        val takePictureIntent = Intent(activity, PanoShooterActivity::class.java)

        var panoFile: File? = null
        try {
            panoFile = createImageFile(activity)
        } catch (exception: IOException) {
            //Timber.e(exception)
            Toast.makeText(context, "Ошибка создания файла")
        }

        if (panoFile != null) {
            val photoURI = getUriForFile(activity, panoFile)
            grantUriPermission(activity, takePictureIntent, photoURI)

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            activity.startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)

            tempImageFilePath = panoFile.absolutePath
        } else {
            exceptionSubject.onNext(Unit)
            notificator.show(R.string.error_open_camera)
        }
    }


    private fun createImageFile(context: Context): File {
        val TEMP_IMAGE_DATE_FORMAT = "yyyyMMdd_HHmmss"
        val timeStamp = SimpleDateFormat(TEMP_IMAGE_DATE_FORMAT).format(System.currentTimeMillis())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            Toast.makeText(context, "Отображение результата", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SimpleCameraFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CamerasFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}