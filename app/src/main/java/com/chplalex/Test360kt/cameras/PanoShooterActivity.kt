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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.galleries.PanoramaActivity
import com.chplalex.Test360kt.galleries.SourceData
import com.chplalex.Test360kt.utils.TAG
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import com.dermandar.dmd_lib.DMD_Capture.ExposureMode
import com.nativesystem.Core
import java.util.*

class PanoShooterActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context, panoUri: Uri) =
            Intent(context, PanoShooterActivity::class.java).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, panoUri)
                context.startActivity(this)
            }

        //Permissions
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 2
        private const val MY_PERMISSIONS_REQUEST_STORAGE = 3

        var locationAsked = false
        private const val SP_NAME = "Test360kt"
    }

    internal enum class detectResult {
        DMDCircleDetectionInvalidInput,
        DMDCircleDetectionCircleNotFound,
        DMDCircleDetectionBad,
        DMDCircleDetectionGood
    }

    private var IS_HD = false
    private val drawcircle = true
    private var mRelativeLayout: RelativeLayout? = null
    private var mWidth = 0
    private var mHeight = 0
    private var mDMDCapture: DMD_Capture? = null
    private var mIsShootingStarted = false
    private var mIsCameraReady = false
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mAspectRatio = 0.0
    private var circle: ImageView? = null
    private var viewGroup: ViewGroup? = null
    private var saveOri = true
    private var mDisplayRotation = 0
    private val request_Code = 103
    private var prefModeKey = "ShotMode"
    private var prefLensKey = "LensSelected"
    private var prefLensNameKey = "LensSelectedName"
    private var selectedLens: String? = "none"
    private var lensName: String? = "None"
    private var txtLensName: TextView? = null
    private var txtShootMode: Button? = null
    private var imgRotMode: ImageView? = null
    private var showYinYang = true
    //used in case when permissions are asked or bluetooth to turn on i requested (onresume is entered before on create sub)
    private val camAdded = false
    private var lensIDRotator = 0
    private var isSDKRotator = false
    private var mNumberTakenImages = 0
    private var mCurrentInstructionMessageID = -1
    private var mTextViewInstruction: TextView? = null
    private lateinit var panoFilePath: String
    private val mIsOpenGallery = false

    //Button startShooting;
    private var FL = 0.0
    private var activityW = 0f
    private var activityH = 0f
    private var isRequestExit = false
    private var isRequestViewer = false

    internal enum class fisheye {
        none
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        Log.d(TAG, "onRequestPermissionsResult main")

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
        var lp: RelativeLayout.LayoutParams

        Log.d(TAG, "oncreatesub")

        //validate location on
        if (!locationAsked) {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            var gps_enabled = false
            var network_enabled = false

            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (ex: Exception) {
                toastMessage("GPS locating isn't enabled")
            }

            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ex: Exception) {
                toastMessage("Network locating isn't enabled")
            }

            if (!gps_enabled && !network_enabled) {
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

        val pref = getSharedPreferences(SP_NAME, MODE_PRIVATE)

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

        //mDisplayMetrics = DisplayMetrics()
        mDisplayRotation = windowManager.defaultDisplay.rotation

        //getting screen resolution
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

        mAspectRatio = mScreenHeight.toDouble() / mScreenWidth.toDouble()

        requestedOrientation = if (mAspectRatio < 1.0)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_shooter)

        mRelativeLayout = findViewById<View>(R.id.relativeLayout) as RelativeLayout

        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels

        startShooter()

        //Text View instruction
        mTextViewInstruction = TextView(this).apply {
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        setInstructionMessage(R.string.tap_anywhere_to_start)
        mRelativeLayout?.addView(
            mTextViewInstruction,
            RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )

        //circle
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        circle = ImageView(this).apply {
            alpha = 0.3f
        }
        if (drawcircle) mRelativeLayout?.addView(circle)

        val shape = GradientDrawable().apply {
            cornerRadius = 50f
            setColor(Color.parseColor("#88a8a8a8"))
        }

        txtLensName = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(20, 20, 20, 20)
            background = shape

            setOnClickListener {
                if (mIsShootingStarted) return@setOnClickListener
                //  if(mDMDCapture.canShootHDR())
                //  mDMDCapture.setHDRStatus(false);
                val intent = Intent(this@PanoShooterActivity, LensesActivity::class.java).apply {
                    putExtra("CurrentLens", selectedLens)
                }
                startActivityForResult(intent, request_Code)
            }
        }

        // You might want to tweak these to WRAP_CONTENT
        lp = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            bottomMargin = 200
            rightMargin = 20
        }

        mRelativeLayout?.addView(txtLensName, lp)

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

        imgRotMode = ImageView(this).apply {
            if (isSDKRotator)
                setImageResource(R.drawable.rotator_disconn)
            else
                setImageResource(R.drawable.handheld)
            //imgRotMode.setBackgroundColor(Color.WHITE);
        }

        // You might want to tweak these to WRAP_CONTENT
        lp = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            bottomMargin = 200
            leftMargin = 70
        }

        mRelativeLayout?.addView(imgRotMode, lp)

        imgRotMode?.setOnClickListener {

            if (mDMDCapture == null) return@setOnClickListener

            isSDKRotator = !isSDKRotator

            pref.edit().apply {
                if (isSDKRotator)
                    putString(prefModeKey, "rotator")
                else
                    putString(prefModeKey, "hand")
                apply()
            }

            if (isSDKRotator)
                imgRotMode?.setImageResource(R.drawable.rotator_disconn)
            else
                imgRotMode?.setImageResource(R.drawable.handheld)

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

            imgRotMode?.isEnabled = false

            Handler().postDelayed({
                progress.dismiss()
                imgRotMode?.isEnabled = true
            }, 2000)
            // end solve fast switch issue
        }

        //setStartShootingHand();
        mDMDCapture?.apply {
            setLensSelected(selectedLens != "none")
            FL = fl
        }

        setNewLens(getLensNb(selectedLens))
    }

    fun getLensNb(selectedLens: String?): Int {
        return fisheye.valueOf(selectedLens!!).ordinal
    }

    fun setNewLens(lensId: Int) {
        txtLensName?.text = lensName
        lensIDRotator = lensId
        mDMDCapture?.setLens(lensId)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")

        super.onPause()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mDMDCapture?.apply {
            if (mIsShootingStarted) {
                stopShooting()
                mIsShootingStarted = false
            }
            stopCamera()
        }
    }

    override fun onResume() {
        Log.d(TAG, "on resume")

        super.onResume()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mDMDCapture?.apply {
            Log.d(TAG, "onResume::startCamera")
            startCamera(this@PanoShooterActivity, mWidth, mHeight)
            //mDMDCapture.setContinuousShooting(true);
        }

        mTextViewInstruction?.visibility = VISIBLE
        imgRotMode?.visibility = VISIBLE
        txtLensName?.visibility = VISIBLE
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        if (mIsShootingStarted) {
            mDMDCapture?.stopShooting()
            mIsShootingStarted = false
            imgRotMode?.visibility = VISIBLE
            txtLensName?.visibility = VISIBLE
        } else {
            isRequestExit = true
            isRequestViewer = false
            mDMDCapture?.releaseShooter()
        }
    }

    private fun startShooter() {
        Log.d(TAG, "startShooter")

        isRequestViewer = false

        mDMDCapture = DMD_Capture().apply{
            setRotatorMode(isSDKRotator)

            if (saveOri) setExportOriOn()

            setCircleDetectionCallback { res ->
                val x: detectResult = detectResult.values().get(res)
                Log.d("TAG", "result detection:$res")
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

        viewGroup = mDMDCapture?.initShooter(this, mCallbackInterface, windowManager.defaultDisplay.rotation, true, true)

        mRelativeLayout!!.addView(viewGroup, 0)

        viewGroup?.setOnClickListener(View.OnClickListener {
            if (!mIsCameraReady) return@OnClickListener

            if (mIsShootingStarted) {
                mDMDCapture?.finishShooting()
                mIsShootingStarted = false
                imgRotMode?.visibility = VISIBLE
                txtLensName?.visibility = VISIBLE
            } else {
                mNumberTakenImages = 0

                mIsShootingStarted = mDMDCapture!!.startShooting(getExternalFilesDir(DIRECTORY_PICTURES).toString())

                if (mIsShootingStarted) {
                    imgRotMode?.visibility = INVISIBLE
                    txtLensName?.visibility = INVISIBLE
                } else {
                    imgRotMode?.visibility = VISIBLE
                    txtLensName?.visibility = VISIBLE
                }
            }
        })
    }

    private fun drawCircle(_resId: Int) {
        val relMain = findViewById<RelativeLayout>(R.id.relativeLayout)

        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        val topOffset = dm.heightPixels - relMain.measuredHeight

        val position = IntArray(2)
        viewGroup?.getLocationOnScreen(position)

        val y = position[1] - topOffset

        runOnUiThread {
            val dim = IntArray(2)
            val dim2 = IntArray(2)

            viewGroup?.apply {
                getLocationInWindow(dim)
                getLocationOnScreen(dim2)
                activityW = width.toFloat()
                activityH = height.toFloat()
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

            circle?.apply {
                setImageResource(_resId)
                layoutParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    leftMargin = -marginLeft //-margin; -marginLeft;
                    rightMargin = -marginRight //-margin; -marginRight;
                    topMargin = marginTop
                    bottomMargin = -marginBottom
                    width = Math.round(circlediameter)
                    height = Math.round(circlediameter)
                }
                visibility = VISIBLE
            }
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
            mRelativeLayout!!.removeView(viewGroup)
            viewGroup = null
            if (isRequestExit) {
                finish()
                return
            }
            mDMDCapture!!.startCamera(this@PanoShooterActivity, mWidth, mHeight)
        }

        override fun onDirectionUpdated(a: Float) {}

        private var tmr: Timer? = null

        override fun preparingToShoot() {}
        override fun canceledPreparingToShoot() {}
        override fun takingPhoto() {}
        override fun shotTakenPreviewReady(bitmapPreview: Bitmap) {}
        override fun photoTaken() {
            mNumberTakenImages++
            if (mNumberTakenImages <= 0) {
                setInstructionMessage(R.string.tap_anywhere_to_start)
            } else if (mNumberTakenImages == 1) {
                setInstructionMessage(R.string.rotate_left_or_right_or_tap_to_restart)
            } else {
                setInstructionMessage(R.string.tap_to_finish_when_ready_or_continue_rotating)
            }
        }

        override fun stitchingCompleted(info: HashMap<String, Any>) {
            Log.d(TAG, "decode logo")

            val op = BitmapFactory.Options()
            op.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.logo, op)
            val bytes = DMDBitmapToRGBA888.imageToRGBA8888(bmp)
            val min_zenith = 45f //specifies the size of the top logo between 0..90 degrees, otherwise it is set to 0
            val min_nadir = 45f //specifies the size of the bottom logo between 0..90 degrees, otherwise it is set to 0

            mDMDCapture?.apply {
                // для использоваия собственного лого
                // val res = setLogo(bytes, min_zenith, min_nadir)

                // to use  default logo (DMD logo).
                val res = setLogo(null, min_zenith, min_nadir)
                Log.d(TAG, "logo set finished: $res")

                genEquiAt(panoFilePath, 800, 0, 0, false, false)
            }
        }

        override fun shootingCompleted(finished: Boolean) {
            Log.d(TAG, "shootingCompleted: $finished")
            if (finished) {
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
            Toast.makeText(this@PanoShooterActivity, "Compass interference", Toast.LENGTH_SHORT).show()
            mIsShootingStarted = false
        }

        override fun onFinishGeneratingEqui() {
            Log.e("rmh", "onFinishGeneratingEqui")
            mIsShootingStarted = false

            //mDMDCapture!!.startCamera(applicationContext, mWidth, mHeight)
            Toast.makeText(this@PanoShooterActivity, "Image saved to $mEquiPath", Toast.LENGTH_LONG).show()
            PanoramaActivity.start(this@PanoShooterActivity, SourceData("Свежее фото", mEquiPath))

            //isRequestExit = true;
            //isRequestViewer = true;
            //mDMDCapture.stopCamera();
            //mDMDCapture.releaseShooter();
            imgRotMode!!.visibility = View.VISIBLE
            txtLensName!!.visibility = View.VISIBLE
        }

        override fun onExposureChanged(mode: ExposureMode) {
            // TODO Auto-generated method stub
        }

        //rotaor
        override fun onRotatorConnected() {
            Log.e("rmh", "rot connected")
            runOnUiThread { //                    txtRotConn.setBackgroundColor(Color.GREEN);
                imgRotMode!!.setImageResource(R.drawable.rotator_conn)
            }
        }

        override fun onRotatorDisconnected() {
            Log.e("rmh", "rot disconnected")
            runOnUiThread { //                    txtRotConn.setBackgroundColor(Color.RED);
                if (isSDKRotator) imgRotMode!!.setImageResource(R.drawable.rotator_disconn)
            }
        }

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