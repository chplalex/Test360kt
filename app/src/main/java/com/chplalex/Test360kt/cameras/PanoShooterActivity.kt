package com.chplalex.Test360kt.cameras

import android.Manifest.permission.*
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
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
import kotlinx.android.synthetic.main.activity_shooter.*
import java.util.*
import kotlin.math.roundToInt

class PanoShooterActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context, panoUri: Uri) =
            Intent(context, PanoShooterActivity::class.java).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, panoUri)
                context.startActivity(this)
            }

        const val PANO_SHOOTER_RESULT_PATH = "PANO_SHOOTER_RESULT_PATH"

        //Permissions
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 2
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 3

        private var locationAsked = false
        private lateinit var mPrefName: String
        private const val REQUEST_CODE_LENSES = 103
    }

    internal enum class detectResult {
        DMDCircleDetectionInvalidInput,
        DMDCircleDetectionCircleNotFound,
        DMDCircleDetectionBad,
        DMDCircleDetectionGood
    }

    private var IS_HD = false
    private var mWidth = 0
    private var mHeight = 0
    private var mDMDCapture: DMD_Capture? = null
    private var mIsShootingStarted = false
    private var mIsCameraReady = false
    private var mViewGroup: ViewGroup? = null
    private var saveOri = true
    private var mDisplayRotation = 0
    private var prefModeKey = "ShotMode"
    private var prefLensKey = "LensSelected"
    private var prefLensNameKey = "LensSelectedName"
    private var selectedLens: String? = "none"
    private var lensName: String? = "None"
    private var lensIDRotator = 0
    private var isSDKRotator = false
    private var mNumberTakenImages = 0
    private var mCurrentInstructionMessageID = -1
    private lateinit var panoFilePath: String

    //Button startShooting;
    private var FL = 0.0
    private var activityW = 0f
    private var activityH = 0f
    private var isRequestExit = false

    internal enum class fisheye {
        none
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefName = applicationContext.packageName

        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val panoUri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
        panoFilePath = panoUri?.path.toString()

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
        Log.e(TAG, "onRequestPermissionsResult() main")

        if ((requestCode != MY_PERMISSIONS_REQUEST_CAMERA) &&
            (requestCode != MY_PERMISSIONS_REQUEST_STORAGE) &&
            (requestCode != MY_PERMISSIONS_REQUEST_LOCATION)
        ) return

        if (SDK_INT < M) return

        if (checkPermissionsAll()) {
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
    private fun checkPermissionsCamera() =
        (checkSelfPermission(CAMERA) == PERMISSION_GRANTED)

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
        Log.e(TAG, "onCreateSub()")

        initLocationMode()
        initShootMode()
        mDisplayRotation = windowManager.defaultDisplay.rotation
        initRequestedOrientation()
        setContentView(R.layout.activity_shooter)
        initDimensions()
        startShooter()
        setInstructionMessage(R.string.tap_anywhere_to_start)
        initLenses()
        initRotatorMode()

        //TODO: что-то с этим сделать
        imageViewCircle.alpha = 0.3f
    }

    private fun initLocationMode() {
        //validate location on
        if (!locationAsked) {
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
                locationAsked = true
            }
        }
        //end validate location on
    }

    private fun initRequestedOrientation() {
        var mScreenWidth = 0
        var mScreenHeight = 0

        val mDisplay = windowManager.defaultDisplay
        val mDisplayMetrics = DisplayMetrics()
        mDisplay.getMetrics(mDisplayMetrics)
        val mDisplayRotation = windowManager.defaultDisplay.rotation

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

    private fun initDimensions() {
        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels
    }

    private fun initShootMode() {
        val pref = getSharedPreferences(mPrefName, MODE_PRIVATE)
        val lastShootMode = pref.getString(prefModeKey, "")
        if (lastShootMode == "")
            pref.edit().apply {
                if (isSDKRotator)
                    putString(prefModeKey, "rotator")
                else
                    putString(prefModeKey, "hand")
                apply()
            }
        else
            isSDKRotator = (lastShootMode == "rotator")
    }

    private fun initLenses() {
        val shape = GradientDrawable().apply {
            cornerRadius = 50f
            setColor(Color.parseColor("#88a8a8a8"))
        }
        textViewLensName.background = shape
        textViewLensName.setOnClickListener {
            if (mIsShootingStarted) return@setOnClickListener
            val intent = Intent(this@PanoShooterActivity, LensesActivity::class.java)
            intent.putExtra("CurrentLens", selectedLens)
            startActivityForResult(intent, REQUEST_CODE_LENSES)
        }

        val pref = getSharedPreferences(mPrefName, MODE_PRIVATE)
        val lastUsedLense = pref.getString(prefLensKey, "none")
        if (lastUsedLense == "") {
            pref.edit().apply {
                putString(prefLensKey, selectedLens)
                apply()
            }
        } else {
            selectedLens = lastUsedLense
            lensName = pref.getString(prefLensNameKey, "Линз нет")
        }

        setNewLens(getLensNb(selectedLens))

        mDMDCapture?.apply {
            setLensSelected(selectedLens != "none")
            FL = fl
        }
    }

    fun getLensNb(lensName: String?): Int {
        return fisheye.valueOf(lensName!!).ordinal
    }

    fun setNewLens(lensId: Int) {
        textViewLensName.text = lensName
        lensIDRotator = lensId
        mDMDCapture?.setLens(lensId)
    }

    private fun initRotatorMode() = imageViewRotator.apply {
        if (isSDKRotator)
            setImageResource(R.drawable.rotator_disconn)
        else
            setImageResource(R.drawable.handheld)

        setOnClickListener {

            if (mDMDCapture == null) return@setOnClickListener

            isSDKRotator = !isSDKRotator

            val pref = getSharedPreferences(mPrefName, MODE_PRIVATE)
            pref.edit().apply {
                if (isSDKRotator)
                    putString(prefModeKey, "rotator")
                else
                    putString(prefModeKey, "hand")
                apply()
            }

            if (isSDKRotator)
                setImageResource(R.drawable.rotator_disconn)
            else
                setImageResource(R.drawable.handheld)

            mDMDCapture?.prepareFlipMode(isSDKRotator)

            // start solve fast switch issue
            val progress = ProgressDialog(this@PanoShooterActivity).apply {
                if (isSDKRotator) {
                    setTitle("Подключение...")
                    setMessage("Подключение к ротатору")
                } else {
                    setTitle("Отключение...")
                    setMessage("Отключение от ротатора")
                }
                setCancelable(false) // disable dismiss by tapping outside of the dialog
                show()
            }

            isEnabled = false

            Handler().postDelayed({
                progress.dismiss()
                isEnabled = true
            }, 2000)
            // end solve fast switch issue
        }
    }

    override fun onPause() {
        Log.e(TAG, "onPause()")

        super.onPause()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mDMDCapture?.apply {
            if (mIsShootingStarted) {
                Log.e(TAG, "onPause() -> stopShooting()")
                stopShooting()
                mIsShootingStarted = false
            }
            Log.e(TAG, "onPause() -> stopCamera()")
            stopCamera()
        }
    }

    override fun onResume() {
        Log.e(TAG, "onResume()")

        super.onResume()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mDMDCapture?.apply {
            Log.e(TAG, "onResume() -> startCamera()")
            startCamera(this@PanoShooterActivity, mWidth, mHeight)
        }

        textViewInstruction.visibility = VISIBLE
        imageViewRotator.visibility = VISIBLE
        textViewLensName.visibility = VISIBLE
    }

    override fun onBackPressed() {
        Log.e(TAG, "onBackPressed()")
        if (mIsShootingStarted) {
            mDMDCapture?.stopShooting()
            mIsShootingStarted = false
            imageViewRotator.visibility = VISIBLE
            textViewLensName.visibility = VISIBLE
        } else {
            isRequestExit = true
            mDMDCapture?.releaseShooter()
        }
    }

    private fun startShooter() {
        Log.e(TAG, "startShooter()")

        initDMDCaptute()
        initViewGroup()
    }

    private fun initDMDCaptute() {
        mDMDCapture = DMD_Capture().apply {
            setRotatorMode(isSDKRotator)

            if (saveOri) setExportOriOn()

            setCircleDetectionCallback { res ->
                Log.e("TAG", "circleDetectionCallback(), result detection:$res")

                val x: detectResult = detectResult.values().get(res)

                if (x == detectResult.DMDCircleDetectionInvalidInput) {
                    toastMessage("Что-то с линзами пошло не так...")
                } else if (x == detectResult.DMDCircleDetectionCircleNotFound) {
                    drawCircle(R.drawable.yellowcircle)
                } else if (x == detectResult.DMDCircleDetectionBad) {
                    drawCircle(R.drawable.redcircle)
                } else if (x == detectResult.DMDCircleDetectionGood) {
                    drawCircle(R.drawable.greencircle)
                }
            }

            if (canShootHD()) {
                setResolutionHD()
                IS_HD = true
            }
        }
    }

    private fun initViewGroup() {
        mViewGroup = mDMDCapture?.initShooter(
            this,
            mCallbackInterface,
            windowManager.defaultDisplay.rotation,
            true,
            true
        )

        mViewGroup?.setOnClickListener(View.OnClickListener {
            if (!mIsCameraReady) return@OnClickListener

            if (mIsShootingStarted) {
                mDMDCapture?.finishShooting()
                mIsShootingStarted = false
                imageViewRotator.visibility = VISIBLE
                textViewLensName.visibility = VISIBLE
            } else {
                mNumberTakenImages = 0

                mIsShootingStarted =
                    mDMDCapture!!.startShooting(getExternalFilesDir(DIRECTORY_PICTURES).toString())

                if (mIsShootingStarted) {
                    imageViewRotator.visibility = INVISIBLE
                    textViewLensName.visibility = INVISIBLE
                } else {
                    imageViewRotator.visibility = VISIBLE
                    textViewLensName.visibility = VISIBLE
                }
            }
        })

        relativeLayout.addView(mViewGroup, 0)
    }

    private fun drawCircle(_resId: Int) = runOnUiThread {
        val dim = IntArray(2)

        mViewGroup?.apply {
            getLocationInWindow(dim)
            activityW = width.toFloat()
            activityH = height.toFloat()
        }

        Log.e(TAG, "drawCircle(), dim:" + dim[0] + " " + dim[1])

        val circleValues = Core.getCircleData(activityW, activityH, 0, dim[0], FL)

        val circleDiameter = circleValues[4]

        val marginTop = (circleValues[0] + dim[1]).roundToInt()
        val marginRight = circleValues[1].roundToInt()
        val marginBottom = circleValues[2].roundToInt()
        val marginLeft = circleValues[3].roundToInt()

        Log.e(TAG, "drawCircle(), mars: $marginLeft $marginRight $marginTop $marginBottom")

        imageViewCircle.apply {
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
            mIsShootingStarted = false
            mIsCameraReady = false
            if (tmr != null) tmr!!.cancel()
            tmr = null
        }

        override fun onCameraStarted() {
            mIsCameraReady = true
        }

        override fun onFinishClear() {}

        override fun onFinishRelease() {
            Log.e(TAG, "onFinishRelease(), isRequestExit = $isRequestExit")

            relativeLayout.removeView(mViewGroup)
            mViewGroup = null

            if (isRequestExit) {
                finish()
                return
            }
        }

        override fun onDirectionUpdated(a: Float) {}

        private var tmr: Timer? = null

        override fun preparingToShoot() {}
        override fun canceledPreparingToShoot() {}
        override fun takingPhoto() {}
        override fun shotTakenPreviewReady(bitmapPreview: Bitmap) {}
        override fun photoTaken() {
            mNumberTakenImages++

            Log.e(TAG, "photoTaken(), mNumberTakenImages = $mNumberTakenImages")

            if (mNumberTakenImages <= 0) {
                setInstructionMessage(R.string.tap_anywhere_to_start)
            } else if (mNumberTakenImages == 1) {
                setInstructionMessage(R.string.rotate_left_or_right_or_tap_to_restart)
            } else {
                setInstructionMessage(R.string.tap_to_finish_when_ready_or_continue_rotating)
            }
        }

        override fun stitchingCompleted(info: HashMap<String, Any>) {
            Log.e(TAG, "stitchingCompleted()")
            for (entry in info) {
                Log.e(TAG, "info.entry = $entry")
            }

            mDMDCapture?.apply {
                Log.e(TAG, "stitchingCompleted() -> genEquiAt()")
                genEquiAt(panoFilePath, 800, 0, 0, false, false)
            }
        }

        override fun shootingCompleted(finished: Boolean) {
            Log.e(TAG, "shootingCompleted(), finished = $finished")
            if (finished) {
                Log.e(TAG, "shootingCompleted() -> stopCamera()")
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
            Log.e(TAG, "compassEvent(), info:")
            for (entry in info) {
                Log.e(TAG, "entry = $entry")
            }
            mIsShootingStarted = false
        }

        override fun onFinishGeneratingEqui() {
            Log.e(TAG, "onFinishGeneratingEqui(), panoFilePath = $panoFilePath")

            val resultIntent = Intent().apply { putExtra(PANO_SHOOTER_RESULT_PATH, panoFilePath) }
            setResult(RESULT_OK, resultIntent)

            mIsShootingStarted = false
            isRequestExit = true
            mDMDCapture?.releaseShooter()
        }

        override fun onExposureChanged(mode: ExposureMode) {  }

        //rotator
        override fun onRotatorConnected() {
            Log.e(TAG, "onRotatorConnected()")
            runOnUiThread {
                imageViewRotator.setImageResource(R.drawable.rotator_conn)
            }
        }

        override fun onRotatorDisconnected() {
            Log.e(TAG, "onRotatorDisconnected()")
            runOnUiThread {
                if (isSDKRotator) imageViewRotator.setImageResource(R.drawable.rotator_disconn)
            }
        }

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