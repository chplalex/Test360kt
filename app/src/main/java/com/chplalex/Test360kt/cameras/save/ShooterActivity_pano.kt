package com.chplalex.Test360kt.cameras.save

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import com.chplalex.Test360kt.R
import com.chplalex.Test360kt.cameras.DMDBitmapToRGBA888
import com.chplalex.Test360kt.cameras.LensesActivity
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import com.dermandar.dmd_lib.DMD_Capture.CircleDetectionCallback
import com.dermandar.dmd_lib.DMD_Capture.ExposureMode
import com.nativesystem.Core
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.*

// исходный код из примера. оставлен для сравнения
class ShooterActivity_pano : Activity() {
    private val drawcircle = true
    private var mRelativeLayout: RelativeLayout? = null
    private var mDMDCapture: DMD_Capture? = null
    private var mIsShootingStarted = false
    private var mIsCameraReady = false
    private var mWidth = 400
    private var mHeight = 500
    private var mPanoramaPath: String? = null
    private var mEquiPath = ""
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mAspectRatio = 0.0
    var circle: ImageView? = null
    var viewGroup: ViewGroup? = null
    var saveOri = true
    private var mDisplayRotation = 0
    val request_Code = 103
    var prefModeKey = "ShotMode"
    var prefLensKey = "LensSelected"
    var prefLensNameKey = "LensSelectedName"
    var selectedLens: String? = "none"
    var lensName: String? = "None"
    var txtLensName: TextView? = null
    var txtShootMode: Button? = null
    var imgRotMode: ImageView? = null
    var showYinYang = true
    private val camAdded = false //used in case when permissions are asked or bluetooth to turn on i requested (onresume is entered before on create sub)

    //Button startShooting;
    var FL = 0.0
    var activityW = 0f
    var activityH = 0f
    var isRequestExit = false
    var isRequestViewer = false

    internal enum class detectResult {
        DMDCircleDetectionInvalidInput, DMDCircleDetectionCircleNotFound, DMDCircleDetectionBad, DMDCircleDetectionGood
    }

    private val circleResult = detectResult.values()

    internal enum class fisheye {
        none
    }

    var camPreview: ViewGroup? = null
    var isSDKRotator = false
    var isHDREnabled = false
    var self = this
    var lensIDRotator = 0
    var isRotatorShooterReady = false
    private var mDisplayMetrics: DisplayMetrics? = null
    private var mCurrentInstructionMessageID = -1
    private var mTextViewInstruction: TextView? = null
    private var mNumberTakenImages = 0
    var REQUEST_ENABLE_BT = 104

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("rmh", "oncreate")

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)


        //onCreateSub();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                validateBluetooth()
                return
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
                return
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_STORAGE)
                return
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
                return
            }
        }
        validateBluetooth()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.e("rmh", "onRequestPermissionsResult main")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (requestCode) {
                MY_PERMISSIONS_REQUEST_CAMERA -> {
                    Log.e("rmh", "MY_PERMISSIONS_REQUEST_CAMERA")
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_STORAGE)
                            return
                        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
                            return
                        } else validateBluetooth()
                    } else {
                        toastMessage("Camera, Storage and Location permissions are not optional!")
                        finish()
                    }
                    return
                }
                MY_PERMISSIONS_REQUEST_STORAGE -> {
                    run {
                        Log.e("rmh", "MY_PERMISSIONS_REQUEST_STORAGE")
                        if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
                                    return
                                }
                                validateBluetooth()
                            } else {
                                toastMessage("Camera, Storage and Location permissions are not optional!")
                                finish()
                                return
                            }
                        } else {
                            toastMessage("Camera, Storage and Location permissions are not optional!")
                            finish()
                            return
                        }
                    }
                    run {
                        Log.e("rmh", "MY_PERMISSIONS_REQUEST_LOCATION")
                        if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                toastMessage("Camera, Storage and Location permissions are not optional!")
                                finish()
                                return
                            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                toastMessage("Camera, Storage and Location permissions are not optional!")
                                finish()
                                return
                            }
                            validateBluetooth()
                        } else {
                            toastMessage("Camera, Storage and Location permissions are not optional!")
                            finish()
                            return
                        }
                    }
                }
                MY_PERMISSIONS_REQUEST_LOCATION -> {
                    Log.e("rmh", "MY_PERMISSIONS_REQUEST_LOCATION")
                    if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            toastMessage("Camera, Storage and Location permissions are not optional!")
                            finish()
                            return
                        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            toastMessage("Camera, Storage and Location permissions are not optional!")
                            finish()
                            return
                        }
                        validateBluetooth()
                    } else {
                        toastMessage("Camera, Storage and Location permissions are not optional!")
                        finish()
                        return
                    }
                }
            }
        }
    }

    private fun validateBluetooth() {
        //onCreateSub();
        val mBluetoothAdapter: BluetoothAdapter?
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            toastMessage("Your phone does not support Bluetooth")
            return
        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            onCreateSub()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                toastMessage("Bluetooth is not optional.\nTurn on bluetooth and restart the app.")
                return
            } else {
                onCreateSub()
            }
        } else if (requestCode == request_Code) {
            if (resultCode == RESULT_OK) {
                circle!!.visibility = View.INVISIBLE
                val lensId = data.getStringExtra("lensId")
                lensName = data.getStringExtra("lensName")
                if (lensId != null) {
                    selectedLens = lensId
                    if (selectedLens == "none") mDMDCapture!!.setLensSelected(false) else mDMDCapture!!.setLensSelected(true)
                    setNewLens(getLensNb(selectedLens))
                }
                Log.e("rmh", "aaaa:$selectedLens")
            }
        }
    }

    private fun onCreateSub() {
        Log.e("rmh", "oncreatesub")
        //validate location on
        if (!locationAsked) {
            val lm = this.getSystemService(LOCATION_SERVICE) as LocationManager
            var gps_enabled = false
            var network_enabled = false
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (ex: Exception) {
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ex: Exception) {
            }
            if (!gps_enabled && !network_enabled) {
                // notify user
                val dialog = AlertDialog.Builder(this)
                dialog.setMessage("Your GPS is turned off, you may need to turn it on to connect to the Bluetooth rotator")
                dialog.setPositiveButton("Open Location Setting") { paramDialogInterface, paramInt ->
                    // TODO Auto-generated method stub
                    val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(myIntent)
                    //get gps
                }
                dialog.setNegativeButton("Ignore") { paramDialogInterface, paramInt ->
                    // TODO Auto-generated method stub
                }
                //dialog.set
                dialog.show()
                locationAsked = true
            }
        }


        //end validate location on
        val pref = applicationContext.getSharedPreferences("MyPref", MODE_PRIVATE)
        val lastShootMode = pref.getString(prefModeKey, "")
        if (lastShootMode == "") {
            val editor = pref.edit()
            if (isSDKRotator) editor.putString(prefModeKey, "rotator") else editor.putString(prefModeKey, "hand")
            editor.commit()
        } else {
            isSDKRotator = if (lastShootMode == "rotator") true else false
        }
        mDisplayMetrics = DisplayMetrics()
        mDisplayRotation = windowManager.defaultDisplay.rotation
        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() + "/" + folderName
        val _path = File(path)
        _path.mkdirs()
        Log.e("rmh", "path:**$path")
        //Core.setLogPath(path);
        //Core.setDebugPathRotator(path);

        //getting screen resolution
        val mDisplay = windowManager.defaultDisplay
        val mDisplayMetrics = DisplayMetrics()
        mDisplay.getMetrics(mDisplayMetrics)
        val mDisplayRotation = windowManager.defaultDisplay.rotation

        //Full screen activity
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (mDisplayRotation == Surface.ROTATION_0 || mDisplayRotation == Surface.ROTATION_180) {
            mScreenWidth = mDisplayMetrics.widthPixels
            mScreenHeight = mDisplayMetrics.heightPixels
        } else {
            mScreenWidth = mDisplayMetrics.heightPixels
            mScreenHeight = mDisplayMetrics.widthPixels
        }
        mAspectRatio = mScreenHeight.toDouble() / mScreenWidth.toDouble()
        requestedOrientation = if (mAspectRatio < 1.0) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContentView(R.layout.activity_shooter)
        mRelativeLayout = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels
        startShooter()
        var lp: RelativeLayout.LayoutParams
        val lp0 = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        //Text View instruction
        mTextViewInstruction = TextView(this)
        mTextViewInstruction!!.textSize = 32f
        mTextViewInstruction!!.gravity = Gravity.CENTER
        mTextViewInstruction!!.setTextColor(Color.WHITE)
        setInstructionMessage(R.string.tap_anywhere_to_start)
        mRelativeLayout!!.addView(mTextViewInstruction, lp0)

        //circle
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        circle = ImageView(this as Context)
        circle!!.alpha = 0.3f
        if (drawcircle) mRelativeLayout!!.addView(circle)
        val shape = GradientDrawable()
        shape.cornerRadius = 50f
        shape.setColor(Color.parseColor("#88a8a8a8"))
        txtLensName = TextView(this)
        txtLensName!!.textSize = 16f
        txtLensName!!.gravity = Gravity.CENTER
        txtLensName!!.setTextColor(Color.BLACK)
        txtLensName!!.setPadding(20, 20, 20, 20)
        txtLensName!!.background = shape
        lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) // You might want to tweak these to WRAP_CONTENT
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        lp.bottomMargin = 200
        lp.rightMargin = 20
        mRelativeLayout!!.addView(txtLensName, lp)
        val lastUsedLense = pref.getString(prefLensKey, "none")
        if (lastUsedLense == "") {
            val editor = pref.edit()
            editor.putString(prefLensKey, selectedLens)
            editor.commit()
        } else {
            selectedLens = lastUsedLense
            lensName = pref.getString(prefLensNameKey, "No Lens")
        }
        imgRotMode = ImageView(this)
        if (isSDKRotator) imgRotMode!!.setImageResource(R.drawable.rotator_disconn) else imgRotMode!!.setImageResource(R.drawable.handheld)
        //imgRotMode.setBackgroundColor(Color.WHITE);
        lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) // You might want to tweak these to WRAP_CONTENT
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lp.bottomMargin = 200
        lp.leftMargin = 70
        mRelativeLayout!!.addView(imgRotMode, lp)
        imgRotMode!!.setOnClickListener {
            //if(mIsShootingStarted) return;
            if (mDMDCapture != null) {
                isSDKRotator = !isSDKRotator
                val pref = applicationContext.getSharedPreferences("MyPref", MODE_PRIVATE)
                val editor = pref.edit()
                if (isSDKRotator) editor.putString(prefModeKey, "rotator") else editor.putString(prefModeKey, "hand")
                editor.commit()
                if (isSDKRotator) {
                    imgRotMode!!.setImageResource(R.drawable.rotator_disconn)
                    mDMDCapture!!.prepareFlipMode(isSDKRotator)
                } else {
                    imgRotMode!!.setImageResource(R.drawable.handheld)
                    mDMDCapture!!.prepareFlipMode(isSDKRotator)
                }

                // start solve fast switch issue
                val progress = ProgressDialog(this@ShooterActivity_pano)
                progress.setTitle((if (isSDKRotator) "C" else "Disc") + "onnecting...")
                progress.setMessage((if (isSDKRotator) "Connecting to" else "Disconnecting from") + " rotator")
                progress.setCancelable(false) // disable dismiss by tapping outside of the dialog
                progress.show()
                imgRotMode!!.isEnabled = false
                val h = Handler()
                h.postDelayed({
                    progress.dismiss()
                    imgRotMode!!.isEnabled = true
                }, 2000)
                // end solve fast switch issue
            }
        }
        lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lp.bottomMargin = 200
        lp.leftMargin = 250
        //if(!isRotatorMode)
        //txtRotConn.setVisibility(View.INVISIBLE);
        //mRelativeLayout.addView(txtRotConn,lp);
        txtLensName!!.setOnClickListener {
            if (!mIsShootingStarted) {
                //if(mDMDCapture.canShootHDR())
                //  mDMDCapture.setHDRStatus(false);
                val a = Intent(this@ShooterActivity_pano, LensesActivity::class.java)
                a.putExtra("CurrentLens", selectedLens)
                startActivityForResult(a, request_Code)
            }
        }

        //setStartShootingHand();
        if (selectedLens == "none") mDMDCapture!!.setLensSelected(false) else mDMDCapture!!.setLensSelected(true)
        FL = mDMDCapture!!.fl
        setNewLens(getLensNb(selectedLens))
    }

    private fun startShooter() {
        Log.e("rmh", "startShooter")
        isRequestViewer = false
        mDMDCapture = DMD_Capture()
        mDMDCapture!!.setRotatorMode(isSDKRotator)
        if (saveOri) mDMDCapture!!.setExportOriOn()
        mDMDCapture!!.setCircleDetectionCallback(CircleDetectionCallback { res ->
            val x = detectResult.values()[res]
            Log.e("rmh", "result detection:$res")
            if (!drawcircle) {
                if (x == detectResult.DMDCircleDetectionInvalidInput) {
                    Toast.makeText(this@ShooterActivity_pano, "Something went wrong in detecting the lens.", Toast.LENGTH_SHORT).show()
                }
                return@CircleDetectionCallback
            }
            if (x == detectResult.DMDCircleDetectionInvalidInput) {
                Toast.makeText(this@ShooterActivity_pano, "Something went wrong in detecting the lens.", Toast.LENGTH_SHORT).show()
            } else if (x == detectResult.DMDCircleDetectionCircleNotFound) {
                drawCircle(R.drawable.yellowcircle)
            } else if (x == detectResult.DMDCircleDetectionBad) {
                drawCircle(R.drawable.redcircle)
            } else if (x == detectResult.DMDCircleDetectionGood) {
                drawCircle(R.drawable.greencircle)
            }
        })
        if (mDMDCapture!!.canShootHD()) {
            mDMDCapture!!.setResolutionHD()
            IS_HD = true
        }
        viewGroup = mDMDCapture!!.initShooter(this, mCallbackInterface, windowManager.defaultDisplay.rotation, true, true)
        mRelativeLayout!!.addView(viewGroup, 0)
        viewGroup?.setOnClickListener(View.OnClickListener {
            if (!mIsCameraReady) return@OnClickListener
            if (!mIsShootingStarted) {
                mNumberTakenImages = 0
                mPanoramaPath = getExternalFilesDir(DIRECTORY_PICTURES).toString() + "/Lib_Test/"
                mIsShootingStarted = mDMDCapture!!.startShooting(mPanoramaPath)
                if (mIsShootingStarted) {
                    imgRotMode!!.visibility = View.INVISIBLE
                    txtLensName!!.visibility = View.INVISIBLE
                } else {
                    imgRotMode!!.visibility = View.VISIBLE
                    txtLensName!!.visibility = View.VISIBLE
                }
                //                    mIsShootingStarted = true;
            } else {
                //Log.e("rmh")
                mDMDCapture!!.finishShooting()
                mIsShootingStarted = false
                imgRotMode!!.visibility = View.VISIBLE
                txtLensName!!.visibility = View.VISIBLE
            }
        })
    }

    private fun toastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun getLensNb(selectedLens: String?): Int {
        return fisheye.valueOf(selectedLens!!).ordinal
    }

    fun setNewLens(lensId: Int) {
        txtLensName!!.text = lensName
        lensIDRotator = lensId
        mDMDCapture!!.setLens(lensId)
    }

    override fun onPause() {
        Log.e("rmh", "onpause")
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (mDMDCapture != null) {
            if (mIsShootingStarted) {
                mDMDCapture!!.stopShooting()
                mIsShootingStarted = false
            }
            mDMDCapture!!.stopCamera()
        }
    }

    override fun onResume() {
        Log.e("rmh", "on resume")
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (mDMDCapture != null) {
            Log.e("AMS", "onResume::startCamera")
            mDMDCapture!!.startCamera(this as Context, mWidth, mHeight)
            //mDMDCapture.setContinuousShooting(true);
        }
        if (mTextViewInstruction != null) mTextViewInstruction!!.visibility = View.VISIBLE
        if (imgRotMode != null) imgRotMode!!.visibility = View.VISIBLE
        if (txtLensName != null) txtLensName!!.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        Log.e("rmh", "onbackpressed")
        if (mIsShootingStarted) {
            mDMDCapture!!.stopShooting()
            mIsShootingStarted = false
            imgRotMode!!.visibility = View.VISIBLE
            txtLensName!!.visibility = View.VISIBLE
        } else {
            isRequestExit = true
            isRequestViewer = false
            mDMDCapture!!.releaseShooter()
            //super.onBackPressed();
        }
    }

    fun myToast(msg: String?) {
        val context = applicationContext
        val duration = Toast.LENGTH_LONG
        val toast = Toast.makeText(context, msg, duration)
        toast.show()
    }

    private val relativeLayoutInfo: Unit
        private get() {
            val relMain = findViewById<View>(R.id.relativeLayout) as RelativeLayout
            activityW = relMain.width.toFloat()
            activityH = relMain.height.toFloat()
        }

    fun drawCircle(_resId: Int) {
        val relMain: View = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        val dm = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(dm)
        val topOffset = dm.heightPixels - relMain.measuredHeight
        val tempView: View? = viewGroup
        val position = IntArray(2)
        tempView!!.getLocationOnScreen(position)
        val y = position[1] - topOffset
        runOnUiThread {
            val dim = IntArray(2)
            viewGroup!!.getLocationInWindow(dim)
            Log.e("rmh", "dim:" + dim[0] + " " + dim[1])
            val dim2 = IntArray(2)
            viewGroup!!.getLocationOnScreen(dim2)
            activityW = viewGroup!!.width.toFloat()
            activityH = viewGroup!!.height.toFloat()
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
            Log.e("rmh", "mars: $marginLeft $marginRight $marginTop $marginBottom")
            //circle
            circle!!.setImageResource(_resId)
            val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            lp.leftMargin = -marginLeft //-margin; -marginLeft;
            lp.rightMargin = -marginRight //-margin; -marginRight;
            lp.topMargin = marginTop
            lp.bottomMargin = -marginBottom
            lp.width = Math.round(circlediameter)
            lp.height = Math.round(circlediameter)
            circle!!.layoutParams = lp
            circle!!.visibility = View.VISIBLE
        }
    }

    var folderName = "Panoramas"
    fun saveDataSph() {
        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() + "/" + folderName
        val fileLinks = File("$path/data.txt")
        try {
            val s = String("count\t1\nfovx\t360\ntype\tsph\n".toByteArray(), "UTF-8" as Charset)
            val x = s.toByteArray()
            val fos = FileOutputStream(fileLinks, false)
            fos.write(x)
            fos.close()
        } catch (e: Exception) {
        }
    }

    fun saveDataCyl(fovx: String) {
        val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() + "/" + folderName
        val fileLinks = File("$path/data.txt")
        try {
            val data = "count\t1\nfovx\t$fovx\ntype\tcyl\n"
            val s = String(data.toByteArray(), "UTF-8" as Charset)
            val x = s.toByteArray()
            val fos = FileOutputStream(fileLinks, false)
            fos.write(x)
            fos.close()
        } catch (e: Exception) {
        }
    }

    var imgName: String? = null
    private val mIsOpenGallery = false
    private val mCallbackInterface: CallbackInterfaceShooter = object : CallbackInterfaceShooter {
        override fun onCameraStopped() {
            mIsShootingStarted = false
            mIsCameraReady = false
            if (tmr != null) tmr!!.cancel()
            tmr = null
        }

        override fun onCameraStarted() {
            mIsCameraReady = true
            //            if(tmr!=null) tmr.cancel(); tmr=null;
//            tmr=new Timer();
//            tmr.scheduleAtFixedRate(new TimerTask() {
//                @Override
//                public void run() {
//					cnt++;
//					if(lastMillis==0)lastMillis=System.nanoTime();
//					if(System.nanoTime()-lastMillis>=1*1000000000) {
//						Log.e("DMD", "fps: " + cnt);
//						cnt=0;
//						lastMillis=System.nanoTime();
//					}
//
//                    HashMap<String, Object> map = mDMDCapture.getIndicators();
//					Log.e("DMD", "fovx:" + map.get(DMD_Capture.ShootingIndicatorsEnum.fovx.toString()));//Integer value in degrees
//					Log.e("DMD", "orientation:" + map.get(DMD_Capture.ShootingIndicatorsEnum.orientation.toString()));//Integer, kPanoramaOrientationLTR = -1, kPanoramaOrientationUnknown = 0, kPanoramaOrientationRTL = 1
            // Log.e("DMD", "percentage:" + map.get(DMD_Capture.ShootingIndicatorsEnum.percentage.toString()));//Double
//					Log.e("DMD", "pitch:" + map.get(DMD_Capture.ShootingIndicatorsEnum.pitch.toString()));//Double
//					Log.e("DMD", "roll:" + map.get(DMD_Capture.ShootingIndicatorsEnum.roll.toString()));//Double
//                }
//            },0, 16);
        }

        override fun onFinishClear() {}
        override fun onFinishRelease() {
            mRelativeLayout!!.removeView(viewGroup)
            viewGroup = null
            if (isRequestExit) {
                finish()
                return
            }
            mDMDCapture!!.startCamera(this@ShooterActivity_pano, mWidth, mHeight)
        }

        private val cnt: Long = 0
        private val lastMillis: Long = 0
        override fun onDirectionUpdated(a: Float) {

//			cnt++;
//			if(lastMillis==0)lastMillis=System.nanoTime();
//			if(System.nanoTime()-lastMillis>1*1000000000) {
//				//Log.e("AMS", "fps: " + cnt);
//				cnt=0;
//				lastMillis=System.nanoTime();
//			}
//			Thread t=new Thread(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						Thread.sleep((long)100);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//
//					HashMap<String, Object> map = mDMDCapture.getIndicators();
////					Log.e("AMS", "fovx:" + map.get(DMD_Capture.ShootingIndicatorsEnum.fovx.toString()));//Integer value in degrees
//					//Log.e("AMS", "orientation:" + map.get(DMD_Capture.ShootingIndicatorsEnum.orientation.toString()));//Integer, kPanoramaOrientationLTR = -1, kPanoramaOrientationUnknown = 0, kPanoramaOrientationRTL = 1
//					//Log.e("AMS", "percentage:" + map.get(DMD_Capture.ShootingIndicatorsEnum.percentage.toString()));//Double
////					Log.e("AMS", "pitch:" + map.get(DMD_Capture.ShootingIndicatorsEnum.pitch.toString()));//Double
////					Log.e("AMS", "roll:" + map.get(DMD_Capture.ShootingIndicatorsEnum.roll.toString()));//Double
////					Log.e("AMS", "roll:" + map.get(DMD_Capture.ShootingIndicatorsEnum.roll.toString()));
//				}
//			});
//			t.start();
        }

        private var tmr: Timer? = null
        override fun preparingToShoot() {
            /***
             * Example about reading the shooting indicators
             */
        }

        override fun canceledPreparingToShoot() {}
        override fun takingPhoto() {}
        override fun shotTakenPreviewReady(bitmapPreview: Bitmap) {}
        override fun photoTaken() {
            Log.e("rmh", "photoTaken")
            val map = mDMDCapture!!.indicators
            mNumberTakenImages++
            if (mNumberTakenImages <= 0) {
                setInstructionMessage(R.string.tap_anywhere_to_start)
            } else if (mNumberTakenImages == 1) {
                setInstructionMessage(R.string.rotate_left_or_right_or_tap_to_restart)
            } else {
                setInstructionMessage(R.string.tap_to_finish_when_ready_or_continue_rotating)
            }

//					Log.e("AMS", "fovx:" + map.get(DMD_Capture.ShootingIndicatorsEnum.fovx.toString()));//Integer value in degrees
//            Log.e("AMS", "orientation:" + map.get(DMD_Capture.ShootingIndicatorsEnum.orientation.toString()));//Integer, kPanoramaOrientationLTR = -1, kPanoramaOrientationUnknown = 0, kPanoramaOrientationRTL = 1
//            Log.e("AMS", "percentage:" + map.get(DMD_Capture.ShootingIndicatorsEnum.percentage.toString()));//Double
        }

        override fun stitchingCompleted(info: HashMap<String, Any>) {
            Log.e("rmh", "stitching completed")
            val time = System.currentTimeMillis()
            imgName = "img_" + java.lang.Long.toString(time) + ".jpg"
            mEquiPath = getExternalFilesDir(DIRECTORY_PICTURES)?.path + "/" + folderName + "/" + imgName
            Log.e("AMS", "decode logo")
            val op = BitmapFactory.Options()
            op.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.logo, op)
            val bytes = DMDBitmapToRGBA888.imageToRGBA8888(bmp)
            val min_zenith = 0f //specifies the size of the top logo between 0..90 degrees, otherwise it is set to 0
            val min_nadir = 0f //specifies the size of the bottom logo between 0..90 degrees, otherwise it is set to 0
            val res = mDMDCapture!!.setLogo(bytes, min_zenith, min_nadir)
            //int res = mDMDCapture.setLogo(null,min_zenith,min_nadir);  // to use  default logo (DMD logo).
            Log.e("AMS", "logo set finished: $res")
            mDMDCapture!!.genEquiAt(mEquiPath, 800, 0, 0, false, false)
            //mDMDCapture.releaseShooter();
        }

        override fun shootingCompleted(finished: Boolean) {
            Log.e("rmh", "shootingCompleted: $finished")
            if (finished) {
                //mTextViewInstruction.setVisibility(View.INVISIBLE);
                mDMDCapture!!.stopCamera()
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
            Toast.makeText(this@ShooterActivity_pano, "Compass interference", Toast.LENGTH_SHORT).show()
            mIsShootingStarted = false
        }

        override fun onFinishGeneratingEqui() {
            Log.e("rmh", "onFinishGeneratingEqui")
            mIsShootingStarted = false
            mDMDCapture!!.startCamera(applicationContext, mWidth, mHeight)
            Toast.makeText(this@ShooterActivity_pano, "Image saved to $mEquiPath", Toast.LENGTH_LONG).show()

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
        runOnUiThread { //                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mDisplayMetrics.widthPixels, RelativeLayout.LayoutParams.WRAP_CONTENT);
//                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
//
//                if (msgID == R.string.instruction_empty || msgID == R.string.hold_the_device_vertically || msgID == R.string.tap_anywhere_to_start
//                        || msgID == R.string.instruction_focusing) {
//                    params.addRule(RelativeLayout.CENTER_VERTICAL);
//                } else {
//                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                }
//
//                mTextViewInstruction.setLayoutParams(params);
            mTextViewInstruction!!.setText(msgID)
            mCurrentInstructionMessageID = msgID
        }
    }

    companion object {
        var IS_HD = false

        //Permissions
        const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        const val MY_PERMISSIONS_REQUEST_CAMERA = 2
        const val MY_PERMISSIONS_REQUEST_STORAGE = 3
        var locationAsked = false

        fun start(context: Context) {
            context.startActivity(Intent(context, ShooterActivity_pano::class.java))
        }


    }
}