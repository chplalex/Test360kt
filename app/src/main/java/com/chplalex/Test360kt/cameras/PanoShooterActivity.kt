package com.chplalex.Test360kt.cameras

import android.Manifest.permission.*
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.utils.TAG
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import com.dermandar.dmd_lib.DMD_Capture.ExposureMode
import com.nativesystem.Core
import kotlinx.android.synthetic.main.activity_lenses.view.*
import kotlinx.android.synthetic.main.activity_shooter.*
import java.util.*
import kotlin.math.roundToInt

class PanoShooterActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context, mResultUri: Uri) =
            Intent(context, PanoShooterActivity::class.java).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, mResultUri)
                context.startActivity(this)
            }

        const val PANO_SHOOTER_RESULT_PATH = "PANO_SHOOTER_RESULT_PATH"

        //Permissions
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 2
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 3

        private var isLocationAsked = false
    }

    internal enum class DetectResult {
        DMDCircleDetectionInvalidInput,
        DMDCircleDetectionCircleNotFound,
        DMDCircleDetectionBad,
        DMDCircleDetectionGood
    }

    // The main properties
    private lateinit var mDMDCapture: DMD_Capture
    private lateinit var mViewGroup: ViewGroup
    private lateinit var mResultPath: String

    // Shooter dimensions
    private var mWidth = 0
    private var mHeight = 0

    // Status flags
    private var isShootingStarted = false
    private var isCameraReady = false
    private var isRequestExit = false

    // Status variables
    private var mNumberTakenImages = 0
    private var mCurrentInstructionMessageID = -1
    private var mFL = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        if (SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val mResultUri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
        if (mResultUri == null) {
            toastMessage("Ошибка: Не задан путь и имя файла для сохранения панорамы")
            setResult(RESULT_CANCELED, null)
            finish()
        } else {
            mResultPath = mResultUri.path.toString()
        }

        if (SDK_INT < M) {
            onCreateSub()
            return
        }

        if (checkPermissionsAll()) {
            onCreateSub()
            return
        }

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

    // Проверяем наличие необходимых разрешений
    // TODO проверить, всё ли из этого нам действительно необходимо
    private fun checkPermissionsAll() =
        if (SDK_INT < M)
            true
        else
            checkPermissionsCamera() && checkPermissionsStorage() && checkPermissionsLocation()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if ((requestCode != MY_PERMISSIONS_REQUEST_CAMERA) &&
            (requestCode != MY_PERMISSIONS_REQUEST_STORAGE) &&
            (requestCode != MY_PERMISSIONS_REQUEST_LOCATION)
        ) return

        if (SDK_INT < M) return

        Log.d(TAG, "onRequestPermissionsResult() permissions checking")

        if (checkPermissionsAll()) {
            Log.d(TAG, "onRequestPermissionsResult() permissions OK")
            onCreateSub()
            return
        }

        if (!checkPermissionsCamera()) {
            if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
                toastMessage("Can't working without camera permission")
                finish()
            } else {
                requestPermissionsCamera()
                return
            }
        }

        if (!checkPermissionsStorage()) {
            if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
                toastMessage("Can't working without storage permissions")
                finish()
            } else {
                requestPermissionsStorage()
                return
            }
        }

        if (!checkPermissionsLocation()) {
            if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
                toastMessage("Can't working without location permissions")
                finish()
            } else {
                requestPermissionsLocation()
                return
            }
        }
    }

    @RequiresApi(M)
    private fun checkPermissionsCamera() = (checkSelfPermission(CAMERA) == PERMISSION_GRANTED)

    @RequiresApi(M)
    private fun checkPermissionsStorage() =
        (checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) &&
                (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED)

    @RequiresApi(M)
    private fun checkPermissionsLocation() =
        (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                checkSelfPermission(ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)

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

    // необходимые права имеются. работаем дальше.
    private fun onCreateSub() {
        Log.d(TAG, "onCreateSub()")

        initLocationMode()
        initRequestedOrientation()
        setContentView(R.layout.activity_shooter)
        initDimensions()
        initDMDCapture()
        initViewGroup()
        setInstructionMessage(R.string.tap_anywhere_to_start)
    }

    private fun initLocationMode() {
        //validate location on
        if (!isLocationAsked) {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            var gpsEnabled = false
            var networkEnabled = false

            try {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (ex: Exception) {
                toastMessage("GPS locating isn't enabled")
            }

            try {
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ex: Exception) {
                toastMessage("Network locating isn't enabled")
            }

            if (!gpsEnabled && !networkEnabled) {
                // notify user
                AlertDialog.Builder(this)
                    .setMessage("Your GPS is turned off, you may need to turn it on to connect to the Bluetooth rotator")
                    .setPositiveButton("Open Location Setting") { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton("Ignore") { _, _ ->
                    }
                    .show()
                isLocationAsked = true
            }
        }
        //end validate location on
    }

    private fun initRequestedOrientation() {
        val mScreenWidth: Int
        val mScreenHeight: Int

        val mDisplayMetrics = getDisplayMetrics()
        val mDisplayRotation = getDisplayRotation()

        if (mDisplayRotation == Surface.ROTATION_0 || mDisplayRotation == Surface.ROTATION_180) {
            mScreenWidth = mDisplayMetrics.widthPixels
            mScreenHeight = mDisplayMetrics.heightPixels
        } else {
            mScreenWidth = mDisplayMetrics.heightPixels
            mScreenHeight = mDisplayMetrics.widthPixels
        }

        val mAspectRatio = mScreenHeight.toDouble() / mScreenWidth.toDouble()

        requestedOrientation = if (mAspectRatio < 1.0)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun getDisplayRotation() = getDisplayInstance().rotation

    private fun getDisplayMetrics() = DisplayMetrics().apply {
        getDisplayInstance().getRealMetrics(this)
    }

    private fun getDisplayInstance() = if (SDK_INT >= Build.VERSION_CODES.R) {
        display!!
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }

    private fun initDimensions() = with(getDisplayMetrics()) {
        mWidth = widthPixels
        mHeight = heightPixels
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")

        super.onPause()

        if (!this::mDMDCapture.isInitialized) {
            Log.d(TAG, "onPause(), mDMDCapture NOT initialized")
            return
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (isShootingStarted) {
            Log.d(TAG, "onPause() -> stopShooting()")
            mDMDCapture.stopShooting()
            isShootingStarted = false
        }

        Log.d(TAG, "onPause() -> stopCamera()")
        mDMDCapture.stopCamera()
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() -> releaseShooter()")
        mDMDCapture.releaseShooter()
        super.onDestroy()
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")

        super.onResume()

        if (!this::mDMDCapture.isInitialized) {
            Log.d(TAG, "onResume(), mDMDCapture NOT initialized")
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Log.d(TAG, "onResume() -> startCamera()")
        mDMDCapture.startCamera(this@PanoShooterActivity, mWidth, mHeight)
        textViewInstruction.visibility = VISIBLE
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed()")
        if (isShootingStarted) {
            Log.d(TAG, "onBackPressed() -> stopShooting()")
            mDMDCapture.stopShooting()
            isShootingStarted = false
        } else {
            isRequestExit = true
            Log.d(TAG, "onBackPressed() -> releaseShooter()")
            mDMDCapture.releaseShooter()
        }
    }

    private fun initDMDCapture() {
        mDMDCapture = DMD_Capture().apply {
            setRotatorMode(false)
            setExportOriOn()
            setCircleDetectionCallback { res ->
                Log.d("TAG", "circleDetectionCallback(), result detection:$res")
                when (DetectResult.values().get(res)) {
                    DetectResult.DMDCircleDetectionInvalidInput -> toastMessage("Что-то с линзами пошло не так...")
                    DetectResult.DMDCircleDetectionCircleNotFound -> drawCircle(R.drawable.yellowcircle)
                    DetectResult.DMDCircleDetectionBad -> drawCircle(R.drawable.redcircle)
                    DetectResult.DMDCircleDetectionGood -> drawCircle(R.drawable.greencircle)
                }
            }
            if (canShootHD()) setResolutionHD()
        }
    }

    private fun initViewGroup() {
        mViewGroup = mDMDCapture.initShooter(
            this,
            mCallbackInterface,
            getDisplayRotation(),
            true,
            true
        )
        mViewGroup.setOnClickListener(OnClickListener {
            if (!isCameraReady) return@OnClickListener

            if (isShootingStarted) {
                isShootingStarted = false
                mDMDCapture.finishShooting()
            } else {
                mNumberTakenImages = 0
                isShootingStarted =
                    mDMDCapture.startShooting(getExternalFilesDir(DIRECTORY_PICTURES).toString())
            }
        })
        relativeLayout.addView(mViewGroup, 0)
    }

    private fun drawCircle(_resId: Int) = runOnUiThread {
        val dim = IntArray(2)
        var activityW: Float
        var activityH: Float

        with(mViewGroup) {
            getLocationInWindow(dim)
            activityW = width.toFloat()
            activityH = height.toFloat()
        }

        Log.d(TAG, "drawCircle(), dim:" + dim[0] + " " + dim[1])

        val circleValues = Core.getCircleData(activityW, activityH, 0, dim[0], mFL)

        val circleDiameter = circleValues[4]

        val marginTop = (circleValues[0] + dim[1]).roundToInt()
        val marginRight = circleValues[1].roundToInt()
        val marginBottom = circleValues[2].roundToInt()
        val marginLeft = circleValues[3].roundToInt()

        Log.d(TAG, "drawCircle(), mars: $marginLeft $marginRight $marginTop $marginBottom")

        with(imageViewCircle) {
            setImageResource(_resId)
            layoutParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                leftMargin = -marginLeft
                rightMargin = -marginRight
                topMargin = marginTop
                bottomMargin = -marginBottom
                width = circleDiameter.roundToInt()
                height = circleDiameter.roundToInt()
            }
            visibility = VISIBLE
        }
    }

    private val mCallbackInterface: CallbackInterfaceShooter = object : CallbackInterfaceShooter {

        override fun onCameraStopped() {
            isShootingStarted = false
            isCameraReady = false
        }

        override fun onCameraStarted() {
            isCameraReady = true
        }

        override fun onFinishClear() {
            Log.d(TAG, "onFinishClear()")
        }

        override fun onFinishRelease() {
            Log.d(TAG, "onFinishRelease(), isRequestExit = $isRequestExit")

            relativeLayout.removeView(mViewGroup)

            if (isRequestExit) {
                Log.d(TAG, "onFinishRelease() -> finish()")
                finish()
                return
            }
        }

        override fun onDirectionUpdated(a: Float) {}
        override fun preparingToShoot() {}
        override fun canceledPreparingToShoot() {}
        override fun takingPhoto() {}
        override fun shotTakenPreviewReady(bitmapPreview: Bitmap) {}
        override fun photoTaken() {
            mNumberTakenImages++

            Log.d(TAG, "photoTaken(), mNumberTakenImages = $mNumberTakenImages")

            when(mNumberTakenImages) {
                0 -> setInstructionMessage(R.string.tap_anywhere_to_start)
                1 -> setInstructionMessage(R.string.rotate_left_or_right_or_tap_to_restart)
                else -> setInstructionMessage(R.string.tap_to_finish_when_ready_or_continue_rotating)
            }
        }

        override fun stitchingCompleted(info: HashMap<String, Any>) {
            Log.d(TAG, "stitchingCompleted()")
            for (entry in info) {
                Log.d(TAG, "info.entry = $entry")
            }
            Log.d(TAG, "stitchingCompleted() -> genEquiAt()")
            mDMDCapture.genEquiAt(mResultPath, 800, 0, 0, false, false)
            Log.d(TAG, "stitchingCompleted() -> releaseShooter()")
            mDMDCapture.releaseShooter()
        }

        override fun shootingCompleted(finished: Boolean) {
            Log.d(TAG, "shootingCompleted(), finished = $finished")
            if (finished) {
                Log.d(TAG, "shootingCompleted() -> stopCamera()")
                mDMDCapture.stopCamera()
            }
            isShootingStarted = false
        }

        override fun deviceVerticalityChanged(isVertical: Int) {
            if (isVertical == 1) {
                if (!isShootingStarted) setInstructionMessage(R.string.tap_anywhere_to_start)
            } else {
                setInstructionMessage(R.string.hold_the_device_vertically)
            }
        }

        override fun compassEvent(info: HashMap<String, Any>) {
            Log.d(TAG, "compassEvent(), info:")
            for (entry in info) {
                Log.d(TAG, "entry = $entry")
            }
            isShootingStarted = false
        }

        override fun onFinishGeneratingEqui() {
            Log.d(TAG, "onFinishGeneratingEqui(), mResultPath = $mResultPath")

            val mResultIntent =
                Intent().apply { putExtra(PANO_SHOOTER_RESULT_PATH, mResultPath) }
            setResult(RESULT_OK, mResultIntent)

            isShootingStarted = false
            isRequestExit = true
        }

        override fun onExposureChanged(mode: ExposureMode) {}
        override fun onRotatorConnected() {}
        override fun onRotatorDisconnected() {}
        override fun onStartedRotating() {}
        override fun onFinishedRotating() {}
    }

    private fun setInstructionMessage(msgID: Int) {
        if (mCurrentInstructionMessageID == msgID) return
        runOnUiThread {
            textViewInstruction.setText(msgID)
            mCurrentInstructionMessageID = msgID
        }
    }

    private fun toastMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}