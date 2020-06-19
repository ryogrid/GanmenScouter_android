package com.ganmen

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.*
import android.os.Bundle
import android.provider.MediaStore.Images
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ganmen.GanmenScouter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/*
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
*/
class GanmenScouter : Activity() {
    private val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    private var mchange_btn: Button? = null
    private var incam_btn: Button? = null
    private var tv_top: TextView? = null

    // カメラインスタンス
    private var mCam: Camera? = null
    private var inCam: Camera? = null

    // カメラプレビュークラス
    private var mCamPreview: CameraPreview? = null

    // 画面タッチの2度押し禁止用フラグ
    private var mIsTake = false
    private var isMaleMode = false
    private var isInCamMode = false
    var rootView_: View? = null
    var root_act: Activity = this
    var called_intent = false

    //InterstitialAd mInterstitialAd;
    var preview: RelativeLayout? = null
    var facePP = FacePPService()
    private var isInitializedUI = false
    private fun hasInCam(): Boolean {
        val numberOfCameras = getNumberOfCameras()
        return if (numberOfCameras >= 2) {
            true
        } else {
            false
        }
    }

    public override fun onStart() {
        super.onStart()
        curentInstance = this
    }

    private val permissions: Unit
        private get() {
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.CAMERA)) {
                    AlertDialog.Builder(this).setMessage("端末の「設定」でカメラの権限を許可してください。")
                            .setPositiveButton("OK") { dialog, id -> }
                            .setNegativeButton("Cancel") { dialog, id -> permissions }.create().show()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE)
                }
            } else {
            }
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(this).setMessage("端末の「設定」でストレージへの書き込みの権限を許可してください。")
                            .setPositiveButton("OK") { dialog, id -> }
                            .setNegativeButton("Cancel") { dialog, id -> permissions }.create().show()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
                }
            } else {
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        permissions

/*
		MobileAds.initialize(this, new OnInitializationCompleteListener() {
			@Override
			public void onInitializationComplete(InitializationStatus initializationStatus) {
				//requestNewInterstitial();
			}
		});

		mInterstitialAd = new InterstitialAd(this);
		mInterstitialAd.setAdUnitId("ca-app-pub-3869533485696941/9899777318");
                requestNewInterstitial();

		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdClosed() {
				requestNewInterstitial();
				setup_cam_and_preview(false);
			}
		});
 */
        setup_cam_and_preview()
    }

    /*
    private void requestNewInterstitial() {
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }
 */
    private fun setup_cam_and_preview() {
        if (true.also { isInitializedUI = it }) {
            val preview = findViewById<View>(R.id.cameraPreview) as RelativeLayout
            preview.removeView(mCamPreview)
        }
        isInitializedUI = true
        while (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        // カメラインスタンスの取得
        try {
            if (mCam != null) {
                mCam!!.stopPreview()
                mCam!!.release()
                mCam = null
            }
            if (inCam != null) {
                inCam!!.stopPreview()
                inCam!!.release()
                inCam = null
            }
            if (isInCamMode && inCam == null) {
                inCam = open(1)
            } else if (mCam == null) {
                mCam = open(0)
            }
        } catch (e: Exception) {
            // エラー
            finish()
        }
        val currentCam = if (isInCamMode) inCam else mCam
        val params = currentCam!!.parameters
        val min_width = -1
        //Picture解像度取得
        val supportedPictureSizes = params.supportedPictureSizes
        if (supportedPictureSizes != null && supportedPictureSizes.size > 1) {
            for (i in supportedPictureSizes.indices) {
                val size = supportedPictureSizes[i]
                //解像度表示
                println("picture width:" + size.width + " height:" + size.height)
                if (min_width == -1) {
                    width = size.width
                    height = size.height
                } else if (size.width < width) {
                    width = size.width
                    height = size.height
                }
            }
        }
        params.setPictureSize(width, height)
        params.setPreviewSize(width, height)
        val preview = findViewById<View>(R.id.cameraPreview) as RelativeLayout
        mCamPreview = CameraPreview(this, currentCam)
        setCameraDisplayOrientation(this, if (isInCamMode) 0 else 1, currentCam)
        preview.addView(mCamPreview)

        // mCamPreview に タッチイベントを設定
        mCamPreview!!.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!mIsTake) {
                    // 撮影中の2度押し禁止用フラグ
                    mIsTake = true
                    currentCam?.autoFocus(autoFocusListener_)
                    // 画像取得
                    // mCam.takePicture(null, null, mPicJpgListener);
                }
            }
            true
        }
        tv_top = TextView(this)
        tv_top!!.setTextColor(Color.RED)
        tv_top!!.text = "　　　　　　　　　　　　　　　画面タッチで測定!"
        preview.addView(tv_top, FrameLayout.LayoutParams.WRAP_CONTENT)
        val layout_buttons = LinearLayout(this)
        layout_buttons.orientation = LinearLayout.VERTICAL
        if (hasInCam()) {
            incam_btn = Button(this)
            if (isInCamMode) {
                incam_btn!!.text = "メインカメラモードへ切替"
            } else {
                incam_btn!!.text = "インカムモードへ切替"
            }
            incam_btn!!.setOnClickListener {
                if (isInCamMode) {
                    isInCamMode = false
                    setup_cam_and_preview()
                } else {
                    isInCamMode = true
                    setup_cam_and_preview()
                }
            }
        }
        layout_buttons.addView(incam_btn)
        mchange_btn = Button(this)
        mchange_btn!!.text = "男性撮影モードへ切替"
        mchange_btn!!.setOnClickListener {
            if (isMaleMode == false) {
                isMaleMode = true
                mchange_btn!!.text = "女性撮影モードへ切替"
            } else {
                isMaleMode = false
                mchange_btn!!.text = "男性撮影モードへ切替"
            }
        }
        layout_buttons.addView(mchange_btn)
        preview.addView(layout_buttons)
    }

    // AF完了時のコールバック
    private val autoFocusListener_ = AutoFocusCallback { success, camera ->
        camera.autoFocus(null)
        camera.takePicture(null, null, mPicJpgListener)
    }

    /**
     * JPEG データ生成完了時のコールバック
     */
    private val mPicJpgListener = PictureCallback { data, camera ->
        if (data == null) {
            return@PictureCallback
        }
        val origBitmap = BitmapFactory.decodeByteArray(
                data, 0, data.size)
        val degrees = getCameraDisplayOrientation(root_act) // 後述のメソッド
        val m = Matrix()
        if (isInCamMode) {
            m.postRotate(270f)
        } else {
            m.postRotate(degrees.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(origBitmap, 0, 0, origBitmap.width, origBitmap.height, m, false)

        // ---
        // もし、画像が大きかったら縮小して読み込む
        // 今回はimageSizeMaxの大きさに合わせる
        val shrinked_bitmap: Bitmap?
        val imageSizeMax = 240
        val imageScaleWidth = rotatedBitmap.width / imageSizeMax.toFloat()
        val imageScaleHeight = rotatedBitmap.height / imageSizeMax.toFloat()

        // もしも、縮小できるサイズならば、縮小して読み込む
        if (imageScaleWidth > 2 && imageScaleHeight > 2) {
            val imageOptions2 = BitmapFactory.Options()

            // 縦横、小さい方に縮小するスケールを合わせる
            val imageScale = Math
                    .floor((if (imageScaleWidth > imageScaleHeight) imageScaleHeight else imageScaleWidth).toDouble()).toInt()

            // inSampleSizeには2のべき上が入るべきなので、imageScaleに最も近く、かつそれ以下の2のべき上の数を探す
            var i = 2
            while (i <= imageScale) {
                imageOptions2.inSampleSize = i
                i *= 2
            }
            val baos = ByteArrayOutputStream()
            rotatedBitmap.compress(CompressFormat.JPEG, 100, baos)
            val rot_bytes = baos.toByteArray()
            val bis = ByteArrayInputStream(rot_bytes)
            shrinked_bitmap = BitmapFactory.decodeStream(bis, null, imageOptions2)
            try {
                bis.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Sample Size: 1/" + imageOptions2.inSampleSize)
        } else {
            shrinked_bitmap = rotatedBitmap
        }
        // ---
        val baos = ByteArrayOutputStream()
        shrinked_bitmap!!.compress(CompressFormat.JPEG, 100, baos)
        val shrinked_data = baos.toByteArray()
        val saveDir = curentInstance!!.filesDir

        // 画像保存パス
        val cal = Calendar.getInstance()
        val sf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val imgPath = saveDir.path + "/" + sf.format(cal.time) + ".jpg"

        // ファイル保存
        var fos: FileOutputStream?
        try {
            fos = FileOutputStream(imgPath, true)
            fos.write(shrinked_data)
            fos.close()

            // アンドロイドのデータベースへ登録
            // (登録しないとギャラリーなどにすぐに反映されないため)
            registAndroidDB(imgPath)
        } catch (e: Exception) {
            println(e)
        }
        fos = null
        if (!isMaleMode) {
            val sup = Supplier {
                val tmp = facePP.measure_similarity(facePP.get_face_id("japanese_bijin.png", assets), facePP.get_face_id(shrinked_data))
                doubleTaskResult = tmp
                tmp
            }
            val future = CompletableFuture.supplyAsync(sup)
            try {
                Thread.sleep(7000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        } else {
            val sup = Supplier {
                val tmp = facePP.measure_similarity(facePP.get_face_id("otoko_ikemen.png", assets), facePP.get_face_id(shrinked_data))
                doubleTaskResult = tmp
                tmp
            }
            val future = CompletableFuture.supplyAsync(sup)
            try {
                Thread.sleep(7000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        var result_val = 50.0 + doubleTaskResult
        result_val = Math.floor(result_val)
        mIsTake = false

        // takePicture するとプレビューが停止するので、再度プレビュースタート
        camera.startPreview()
        try {
            called_intent = true
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "text/plain"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(Intent.EXTRA_TEXT, "顔面偏差値 " + result_val.toString() + "点でした! http://bit.ly/1NbvhcO  #顔面スカウター")
            startActivityForResult(Intent.createChooser(intent, result_val.toString() + "点を共有"), 101)
        } catch (e: Exception) {
        }
        /*
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
*/
    }

    /**
     * アンドロイドのデータベースへ画像のパスを登録
     * @param path 登録するパス
     */
    private fun registAndroidDB(path: String) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        val values = ContentValues()
        val contentResolver = this@GanmenScouter.contentResolver
        values.put(Images.Media.MIME_TYPE, "image/jpeg")
        values.put("_data", path)
        contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    override fun onResume() {
        super.onResume()
        if (isInCamMode) {
            inCam!!.startPreview()
        } else {
            mCam!!.startPreview()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val REQUEST_CODE = 1

        // プレビューサイズ
        private var width = -1
        private var height = -1
        var doubleTaskResult = 0.0
        var stringTaskResult: String? = null
        var curentInstance: GanmenScouter? = null
        fun setCameraDisplayOrientation(activity: Activity,
                                        cameraId: Int, camera: Camera?) {
            val info = CameraInfo()
            getCameraInfo(cameraId, info)
            val rotation = activity.windowManager.defaultDisplay
                    .rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }
            var result: Int
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360 // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360
            }
            camera!!.setDisplayOrientation(result)
        }

        private fun getCameraDisplayOrientation(activity: Activity): Int {
            val rotation = activity.windowManager.defaultDisplay.rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }
            return (90 + 360 - degrees) % 360
        }
    }
}