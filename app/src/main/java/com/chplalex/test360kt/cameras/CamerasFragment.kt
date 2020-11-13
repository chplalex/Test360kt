package com.chplalex.test360kt.cameras

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.test360kt.R

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
        false)?.also{

        activity?.title = resources.getString(R.string.label_cameras)

        it.findViewById<ImageButton>(R.id.imgCamSimple)?.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { startActivityForResult(it, CAMERA_REQUEST) }
        }

        it.findViewById<ImageButton>(R.id.imgCamSimpleRight)?.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { startActivityForResult(it, CAMERA_REQUEST) }
        }

        it.findViewById<ImageButton>(R.id.imgCamFull)?.setOnClickListener {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also { startActivityForResult(it, CAMERA_REQUEST) }
        }

        it.findViewById<ImageButton>(R.id.imgCamFullRight)?.setOnClickListener {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).also { startActivityForResult(it, CAMERA_REQUEST) }
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDK)?.setOnClickListener {
            ShooterActivity.start(it.context)
        }

        it.findViewById<ImageButton>(R.id.imgCameraPSDKRight)?.setOnClickListener {
            ShooterActivity.start(it.context)
        }

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