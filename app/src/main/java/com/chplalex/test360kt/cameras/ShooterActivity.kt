package com.chplalex.test360kt.cameras

import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.test360kt.R
import com.chplalex.test360kt.utils.TAG
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import com.dermandar.dmd_lib.DMD_Capture.ExposureMode
import com.nativesystem.Core
import java.util.*

class ShooterActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) =
            Intent(context, ShooterActivity::class.java).apply {
                context.startActivity(this)
            }

        //Permissions
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 2
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 3
    }

    internal enum class detectResult {
        DMDCircleDetectionInvalidInput,
        DMDCircleDetectionCircleNotFound,
        DMDCircleDetectionBad,
        DMDCircleDetectionGood
    }

    private var mRelativeLayout: RelativeLayout? = null
    private var mWidth = 0
    private var mHeight = 0
    private var mDMDCapture: DMD_Capture? = null
    private var mIsShootingStarted = false
    private var mIsCameraReady = false
    private var mPanoramaPath: String? = null
    private var mEquiPath = ""
    private val mScreenWidth = 0
    private var mScreenHeight = 0
    private val mAspectRatio = 0.0
    private var circle: ImageView? = null
    private var viewGroup: ViewGroup? = null
    private var saveOri = true
    private val mDisplayRotation = 0
    private val request_Code = 103
    private var prefModeKey = "ShotMode"
    private var prefLensKey = "LensSelected"
    private var prefLensNameKey = "LensSelectedName"
    private var selectedLens = "none"
    private var lensName = "None"
    private var txtLensName: TextView? = null
    private var txtShootMode: Button? = null
    private var imgRotMode: ImageView? = null
    private var showYinYang = true
    private val camAdded =
        false //used in case when permissions are asked or bluetooth to turn on i requested (onresume is entered before on create sub)
    private var mNumberTakenImages = 0
    private var mCurrentInstructionMessageID = -1
    private val mTextViewInstruction: TextView? = null
    private var imgName: String? = null
    private var folderName = "Panoramas"
    private val mIsOpenGallery = false

    //Button startShooting;
    private var FL = 0.0
    private var activityW = 0f
    private var activityH = 0f
    private var isRequestExit = false
    private var isRequestViewer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        checkPermissions()
        onCreateSub()
    }

    // Проверяем наличие необходимых разрешений
    // TODO проверить, всё ли из этого нам действительно необходимо
    private fun checkPermissions() {
        if (SDK_INT < M) return

        if (!checkPermissionsCamera()) {
            requestPermissionsCamera()
            return
        }

        if (!checkPermissionsStorage()) {
            requestPermissionsStorage()
            return
        }

        if (!checkPermissionsLocation()) {
            requestPermissionsLocation()
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionsResult main")

        if (SDK_INT < M) return

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                Log.d(TAG, "MY_PERMISSIONS_REQUEST_CAMERA")
                // If request is cancelled, the result arrays are empty.
                if (!checkPermissionsCamera()) {
                    toastMessage("Camera permissions are not optional!")
                    finish()
                    return
                }
                if (!checkPermissionsStorage()) {
                    requestPermissionsStorage()
                    return
                }
                if (!checkPermissionsLocation()) {
                    requestPermissionsLocation()
                    return
                }
                return
            }
            MY_PERMISSIONS_REQUEST_STORAGE -> {
                Log.d(TAG, "MY_PERMISSIONS_REQUEST_STORAGE")
                if (!checkPermissionsCamera()) {
                    toastMessage("Camera permissions are not optional!")
                    finish()
                    return
                }
                if (!checkPermissionsStorage()) {
                    toastMessage("Storage permissions are not optional!")
                    finish()
                    return
                }
                if (!checkPermissionsLocation()) {
                    requestPermissionsLocation()
                    return
                }
                return
            }
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                Log.d(TAG, "MY_PERMISSIONS_REQUEST_LOCATION")
                if (!checkPermissionsCamera()) {
                    toastMessage("Camera permissions are not optional!")
                    finish()
                    return
                }
                if (!checkPermissionsStorage()) {
                    toastMessage("Storage permissions are not optional!")
                    finish()
                    return
                }
                if (!checkPermissionsLocation()) {
                    toastMessage("Location permissions are not optional!")
                    finish()
                    return
                }
            }
        }
    }

    @RequiresApi(M)
    private fun checkPermissionsCamera() =
        (checkSelfPermission(CAMERA) == PERMISSION_GRANTED)

    @RequiresApi(M)
    private fun checkPermissionsStorage() =
        (checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) &&
                (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED)

    @RequiresApi(M)
    private fun checkPermissionsLocation() =
        (checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED &&
                checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED)

    @RequiresApi(M)
    private fun requestPermissionsCamera() = requestPermissions(
        arrayOf(CAMERA),
        MY_PERMISSIONS_REQUEST_CAMERA
    )

    @RequiresApi(M)
    private fun requestPermissionsStorage() = requestPermissions(
        arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
        MY_PERMISSIONS_REQUEST_STORAGE
    )

    @RequiresApi(M)
    private fun requestPermissionsLocation() = requestPermissions(
        arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
        MY_PERMISSIONS_REQUEST_LOCATION
    )

    private fun grantResultsOk(grantResults: IntArray, paramCount: Int): Boolean {
        var result = false
        for (r in grantResults) {
            result = result && (r == PERMISSION_GRANTED)
        }
        return result && (grantResults.size == paramCount)
    }

    private fun toastMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // необходимые права имеются. работаем дальше.
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
//        var isRequestViewer = false
        var IS_HD = false
//        var viewGroup: ViewGroup? = null

        mDMDCapture = DMD_Capture()
        mDMDCapture?.setRotatorMode(false)
        mDMDCapture?.setExportOriOn()

        mDMDCapture?.setCircleDetectionCallback { res ->
            val x: detectResult = detectResult.values().get(res)
            Log.d(TAG, "result detection:$res")
            if (x == detectResult.DMDCircleDetectionInvalidInput) {
                toastMessage("Something went wrong in detecting the lens.")
            } else if (x == ShooterActivity.detectResult.DMDCircleDetectionCircleNotFound) {
                drawCircle(R.drawable.yellowcircle)
            } else if (x == ShooterActivity.detectResult.DMDCircleDetectionBad) {
                drawCircle(R.drawable.redcircle)
            } else if (x == ShooterActivity.detectResult.DMDCircleDetectionGood) {
                drawCircle(R.drawable.greencircle)
            }
        }

        mDMDCapture?.let {
            it.canShootHD()
            it.setResolutionHD()
            IS_HD = true
        }

        viewGroup = mDMDCapture?.initShooter(
            this,
            mCallbackInterface,
            windowManager.defaultDisplay.rotation,
            true,
            true
        )
        mRelativeLayout!!.addView(viewGroup, 0)

        viewGroup?.setOnClickListener {
            if (mIsCameraReady) {
                if (!mIsShootingStarted) {
                    mNumberTakenImages = 0
                    mPanoramaPath =
                        Environment.getExternalStorageDirectory().toString() + "/Lib_Test/"
                    mDMDCapture?.let { mIsShootingStarted = it.startShooting(mPanoramaPath) }
                    if (mIsShootingStarted) {
                        imgRotMode?.setVisibility(View.INVISIBLE)
                        txtLensName?.setVisibility(View.INVISIBLE)
                    } else {
                        imgRotMode?.setVisibility(View.VISIBLE)
                        txtLensName?.setVisibility(View.VISIBLE)
                    }
                } else {
                    mDMDCapture?.finishShooting()
                    mIsShootingStarted = false
                    imgRotMode?.setVisibility(View.VISIBLE)
                    txtLensName?.setVisibility(View.VISIBLE)
                }
            }
        }
    }

    fun drawCircle(_resId: Int) {
        val relMain: View = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        val dm = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(dm)
        val topOffset = dm.heightPixels - relMain.measuredHeight
        val tempView = viewGroup
        val position = IntArray(2)
        tempView?.getLocationOnScreen(position)
        val y = position[1] - topOffset
        runOnUiThread {
            val dim = IntArray(2)
            val dim2 = IntArray(2)

            viewGroup?.let {
                it.getLocationInWindow(dim)
                it.getLocationOnScreen(dim2)
                activityW = it.getWidth().toFloat()
                activityH = it.getHeight().toFloat()
            }

            Log.d(TAG, "dim:" + dim[0] + " " + dim[1])

            val marginLeft: Int
            val marginRight: Int
            val marginTop: Int
            val marginBottom: Int
            val circlediameter: Float
            val circleValues: FloatArray

            circleValues = Core.getCircleData(activityW, activityH, 0, dim[0], FL)

            marginTop = Math.round(circleValues[0] + dim[1])
            marginRight = Math.round(circleValues[1])
            marginBottom = Math.round(circleValues[2])
            marginLeft = Math.round(circleValues[3])

            circlediameter = circleValues[4]

            Log.d(TAG, "mars: $marginLeft $marginRight $marginTop $marginBottom")

            circle?.let { itс ->
                val lp = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
                    it.leftMargin = -marginLeft //-margin; -marginLeft;
                    it.rightMargin = -marginRight //-margin; -marginRight;
                    it.topMargin = marginTop
                    it.bottomMargin = -marginBottom
                    it.width = Math.round(circlediameter)
                    it.height = Math.round(circlediameter)
                }
                itс.setImageResource(_resId)
                itс.setLayoutParams(lp)
                itс.setVisibility(View.VISIBLE)
            }
        }
    }

    private val mCallbackInterface: CallbackInterfaceShooter = object : CallbackInterfaceShooter {

        override fun onCameraStopped() {
            mIsShootingStarted = false
            mIsCameraReady = false
            timer?.let { it.cancel() }
            timer = null
        }

        override fun onCameraStarted() {
            mIsCameraReady = true
        }

        override fun onFinishClear() {}

        override fun onFinishRelease() {
            mRelativeLayout?.removeView(viewGroup)
            viewGroup = null
            if (isRequestExit) {
                finish()
                return
            }
            mDMDCapture?.startCamera(this@ShooterActivity, mWidth, mHeight)
        }

        private val cnt: Long = 0
        private val lastMillis: Long = 0
        private var timer: Timer? = null

        override fun onDirectionUpdated(a: Float) {}
        override fun preparingToShoot() {}
        override fun canceledPreparingToShoot() {}
        override fun takingPhoto() {}
        override fun shotTakenPreviewReady(bitmapPreview: Bitmap) {}

        override fun photoTaken() {
            mNumberTakenImages++
            Log.d(TAG, "photoTaken $mNumberTakenImages")
            if (mNumberTakenImages <= 0) {
                setInstructionMessage(R.string.tap_anywhere_to_start)
            } else if (mNumberTakenImages == 1) {
                setInstructionMessage(R.string.rotate_left_or_right_or_tap_to_restart)
            } else {
                setInstructionMessage(R.string.tap_to_finish_when_ready_or_continue_rotating)
            }
        }

        override fun stitchingCompleted(info: HashMap<String, Any>) {
            Log.e(TAG, "stitching completed")
            val time = System.currentTimeMillis()
            imgName = "img_" + java.lang.Long.toString(time) + ".jpg"
            mEquiPath =
                Environment.getExternalStorageDirectory().path + "/" + folderName + "/" + imgName
            Log.e("AMS", "decode logo")
            val op = BitmapFactory.Options()
            op.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.logo, op)
            val bytes = DMDBitmapToRGBA888.imageToRGBA8888(bmp)

            val min_zenith =
                0f //specifies the size of the top logo between 0..90 degrees, otherwise it is set to 0
            val min_nadir =
                0f //specifies the size of the bottom logo between 0..90 degrees, otherwise it is set to 0
            val res = mDMDCapture?.setLogo(bytes, min_zenith, min_nadir)
            // to use  default logo (DMD logo).
            //val res: Int = mDMDCapture.setLogo(null,min_zenith,min_nadir)
            Log.d(TAG, "logo set finished: $res")
            mDMDCapture?.genEquiAt(mEquiPath, 800, 0, 0, false, false)
            //mDMDCapture.releaseShooter();
        }

        override fun shootingCompleted(finished: Boolean) {
            Log.d(TAG, "shootingCompleted: $finished")
            if (finished) {
                //mTextViewInstruction.setVisibility(View.INVISIBLE);
                mDMDCapture?.stopCamera()
            }
            mIsShootingStarted = false
        }

        override fun deviceVerticalityChanged(isVertical: Int) {
            if (isVertical == 1) {
                if (!mIsShootingStarted) setInstructionMessage(R.string.tap_anywhere_to_start)
            } else {
                setInstructionMessage(R.string.hold_the_device_vertically)
            }
        }

        override fun compassEvent(info: HashMap<String, Any>) {
            toastMessage("Compass interference")
            mIsShootingStarted = false
        }

        override fun onFinishGeneratingEqui() {
            Log.d(TAG, "onFinishGeneratingEqui")
            mIsShootingStarted = false
            mDMDCapture?.startCamera(applicationContext, mWidth, mHeight)
            toastMessage("Image saved to $mEquiPath")

            //isRequestExit = true;
            //isRequestViewer = true;
            //mDMDCapture.stopCamera();
            //mDMDCapture.releaseShooter();
            imgRotMode?.setVisibility(View.VISIBLE)
            txtLensName?.setVisibility(View.VISIBLE)
        }

        override fun onExposureChanged(mode: ExposureMode) {
            // TODO: Auto-generated method stub
        }

        //rotator
        override fun onRotatorConnected() {}
        override fun onRotatorDisconnected() {}
        override fun onStartedRotating() {}
        override fun onFinishedRotating() {}
    }

    private fun setInstructionMessage(msgID: Int) {
        if (mCurrentInstructionMessageID == msgID) return
        runOnUiThread {
            mTextViewInstruction?.setText(msgID)
            mCurrentInstructionMessageID = msgID
        }
    }


}