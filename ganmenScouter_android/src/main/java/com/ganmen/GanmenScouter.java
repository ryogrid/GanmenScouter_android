package com.ganmen;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
/*
import com.ganmen.AnalyticsApplication;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener;
*/

public class GanmenScouter extends Activity {
	private final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
	static final int REQUEST_CODE = 1;
	
    // プレビューサイズ
    static private int width = -1;
    static private int height = -1;
    
	private Button mchange_btn;
	private Button incam_btn;

	private TextView tv_top;
	
    // カメラインスタンス
    private Camera mCam = null;
    private Camera inCam = null;

    // カメラプレビュークラス
    private CameraPreview mCamPreview = null;

    // 画面タッチの2度押し禁止用フラグ
    private boolean mIsTake = false;
    
    private boolean isMaleMode = false;
    private boolean isInCamMode = false;
    
    View rootView_ = null;
    
    Activity root_act = this; 
    
    boolean called_intent = false;
 
	InterstitialAd mInterstitialAd;
    
    RelativeLayout preview;

    FacePPService facePP;

    private boolean hasInCam(){
    	int numberOfCameras = Camera.getNumberOfCameras();
    	if(numberOfCameras == 2){
    		return true;
    	}else{
        	return false;    		
    	}
    }

    @Override
    public void onStart() {
      super.onStart();
    }

    private void getPermissions(){
		// Here, thisActivity is the current activity
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {

			// Permission is not granted
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.CAMERA)) {
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				(new AlertDialog.Builder(this)).setMessage("端末の「設定」でカメラの権限を許可してください。")
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// FIRE ZE MISSILES!
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// User cancelled the dialog
							}
						}).create().show();
			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);

				// REQUEST_CODE is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		} else {
			// Permission has already been granted
		}

		// Here, thisActivity is the current activity
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			// Permission is not granted
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				(new AlertDialog.Builder(this)).setMessage("端末の「設定」でストレージへの書き込みの権限を許可してください。")
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// FIRE ZE MISSILES!
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// User cancelled the dialog
							}
						}).create().show();
			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);

				// REQUEST_CODE is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		} else {
			// Permission has already been granted
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
		getPermissions();

		MobileAds.initialize(this, new OnInitializationCompleteListener() {
			@Override
			public void onInitializationComplete(InitializationStatus initializationStatus) {
				requestNewInterstitial();

				AdView mAdView = (AdView) findViewById(R.id.adView);
				AdRequest adRequest = new AdRequest.Builder().build();
				mAdView.loadAd(adRequest);
			}
		});

		mInterstitialAd = new InterstitialAd(this);
		mInterstitialAd.setAdUnitId("ca-app-pub-3869533485696941/9899777318");

		mInterstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdClosed() {
				requestNewInterstitial();
				if(mCam == null){
					setup_in_cam_and_preview();
				}else{
					setup_cam_and_preview(false);
				}
			}
		});

        setup_cam_and_preview(true);

		preview = (RelativeLayout) findViewById(R.id.cameraPreview);

		tv_top = new TextView(this);
		tv_top.setTextColor(Color.RED);
		tv_top.setText("　　　　　　　　　　　　　　　画面タッチで測定!");
		preview.addView(tv_top, LayoutParams.WRAP_CONTENT);

		incam_btn = (Button) findViewById(R.id.incam_btn);
		if (hasInCam()) {
			incam_btn.setText("インカムモードへ切替");
			incam_btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (isInCamMode == false) {
						isInCamMode = true;
//						incam_btn.setText("背面カメラモードへ切替");
						incam_btn.setVisibility(View.INVISIBLE);
						setup_in_cam_and_preview();
					}
				}
			});
		} else {
			incam_btn.setText("インカム無効");
		}

    }
    
    private void requestNewInterstitial() {
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

	private void setup_cam_and_preview(boolean is_first){
		if(is_first==false){
			RelativeLayout preview = (RelativeLayout) findViewById(R.id.cameraPreview);
			preview.removeView(mCamPreview);			
		}
		
        // カメラインスタンスの取得
        try {
        	if(mCam!=null){
                mCam.stopPreview();
                mCam.release();
                mCam = null;	
        	}
        	if(mCam == null){
                mCam = Camera.open(0);        		
        	}
        } catch (Exception e) {
            // エラー
            this.finish();
        }

        Camera.Parameters params = mCam.getParameters();
        
        int min_width = -1;	
        //Picture解像度取得
        List<Size> supportedPictureSizes = params.getSupportedPictureSizes();
        if (supportedPictureSizes != null && supportedPictureSizes.size() > 1) {
            for(int i=0;i<supportedPictureSizes.size();i++){
                Size size = supportedPictureSizes.get(i);
                //解像度表示
                System.out.println("picture width:"+size.width+" height:"+size.height);
                if(min_width == -1){
                	width = size.width;
                	height = size.height;
                }else if(size.width < width){
                	width = size.width;
                	height = size.height;                	
                }
            }
        }           
        
        params.setPictureSize(width, height);
        params.setPreviewSize(width, height);


		RelativeLayout preview = (RelativeLayout) findViewById(R.id.cameraPreview);
		
		mCamPreview = new CameraPreview(this, mCam);

		preview.addView(mCamPreview);
			
		// mCamPreview に タッチイベントを設定
		mCamPreview.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (!mIsTake) {
						// 撮影中の2度押し禁止用フラグ
						mIsTake = true;
						if (mCam != null) {
							// 撮影実行(AF開始)
							mCam.autoFocus(autoFocusListener_);
						}
						// 画像取得
						// mCam.takePicture(null, null, mPicJpgListener);
					}
				}
				return true;
			}
		});

		tv_top = new TextView(this);
		tv_top.setTextColor(Color.RED);
		tv_top.setText("　　　　　　　　　　　　　　　画面タッチで測定!");
		preview.addView(tv_top, LayoutParams.WRAP_CONTENT);
		
		mchange_btn = new Button(this);
		mchange_btn.setText("男性撮影モードへ切替");
		mchange_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(isMaleMode == false){
					isMaleMode = true;
					mchange_btn.setText("女性撮影モードへ切替");					
				}else{
					isMaleMode = false;
					mchange_btn.setText("男性撮影モードへ切替");					
				}
			}
		});
		preview.addView(mchange_btn);
	}

	 public static void setCameraDisplayOrientation(Activity activity,
	         int cameraId, android.hardware.Camera camera) {
	     android.hardware.Camera.CameraInfo info =
	             new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(cameraId, info);
	     int rotation = activity.getWindowManager().getDefaultDisplay()
	             .getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     camera.setDisplayOrientation(result);
	 }
	 
	private void setup_in_cam_and_preview(){        
		RelativeLayout preview = (RelativeLayout) findViewById(R.id.cameraPreview);
		preview.removeView(mCamPreview);
		
		// カメラインスタンスの取得
        try {
            // 現在利用しているカメラを解放
            if (mCam != null) {
                mCam.stopPreview();
                mCam.release();
                mCam = null;            
            }
            if (inCam != null) {
            	inCam.stopPreview();
            	inCam.release();
            	inCam = null;
            }            
            inCam = Camera.open(1);
            
        } catch (Exception e) {
            // エラー
            this.finish();
            System.out.println();
        }

        Camera.Parameters params = inCam.getParameters();
        
        int min_width = -1;	
        //Picture解像度取得
        List<Size> supportedPictureSizes = params.getSupportedPictureSizes();
        if (supportedPictureSizes != null && supportedPictureSizes.size() > 1) {
            for(int i=0;i<supportedPictureSizes.size();i++){
                Size size = supportedPictureSizes.get(i);
                //解像度表示
                System.out.println("picture width:"+size.width+" height:"+size.height);
                if(min_width == -1){
                	width = size.width;
                	height = size.height;
                }else if(size.width < width){
                	width = size.width;
                	height = size.height;                	
                }
            }
        }           
        
        params.setPictureSize(width, height);
        params.setPreviewSize(width, height);
//        inCam.setParameters(params);
		
		mCamPreview = new CameraPreview(this, inCam);

        setCameraDisplayOrientation(this, 1, inCam);
        
		preview.addView(mCamPreview);
		mchange_btn = new Button(this);
		mchange_btn.setText("男性撮影モードへ切替");
		mchange_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(isMaleMode == false){
					isMaleMode = true;
					mchange_btn.setText("女性撮影モードへ切替");					
				}else{
					isMaleMode = false;
					mchange_btn.setText("男性撮影モードへ切替");					
				}

			}
		});
		preview.addView(mchange_btn);
		tv_top = new TextView(this);
		tv_top.setTextColor(Color.RED);
		tv_top.setText("　　　　　　　　　　　　　　　画面タッチで測定!");
		preview.addView(tv_top, LayoutParams.WRAP_CONTENT);		
		
			
		// mCamPreview に タッチイベントを設定
		mCamPreview.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (!mIsTake) {
						// 撮影中の2度押し禁止用フラグ
						mIsTake = true;
						if (inCam != null) {
							// 撮影実行(AF開始)
							inCam.autoFocus(autoFocusListener_);
						}
						// 画像取得
						// mCam.takePicture(null, null, mPicJpgListener);
					}
				}
				return true;
			}
		});
	}	

    // AF完了時のコールバック
    private Camera.AutoFocusCallback autoFocusListener_ = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            camera.autoFocus(null);
            if(mCam == null){
            	inCam.takePicture(null, null, mPicJpgListener);            	
            }else{
            	mCam.takePicture(null, null, mPicJpgListener);	
            }
            
        }
    };

    /**
     * JPEG データ生成完了時のコールバック
     */
    private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
    	
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null) {
                return;
            }
            
            Bitmap origBitmap = BitmapFactory.decodeByteArray(
                    data, 0, data.length);            
            int degrees = getCameraDisplayOrientation(root_act); // 後述のメソッド
            Matrix m = new Matrix();
            
            if(mCam == null){
            	m.postRotate(270);
            }else{
                m.postRotate(degrees);            	
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(origBitmap, 0, 0, origBitmap.getWidth(), origBitmap.getHeight(), m, false);
            
			// ---

			// もし、画像が大きかったら縮小して読み込む
			// 今回はimageSizeMaxの大きさに合わせる
			Bitmap shrinked_bitmap;
			int imageSizeMax = 240;
			float imageScaleWidth = rotatedBitmap.getWidth() / imageSizeMax;
			float imageScaleHeight = rotatedBitmap.getHeight() / imageSizeMax;

			// もしも、縮小できるサイズならば、縮小して読み込む
			if (imageScaleWidth > 2 && imageScaleHeight > 2) {
				BitmapFactory.Options imageOptions2 = new BitmapFactory.Options();

				// 縦横、小さい方に縮小するスケールを合わせる
				int imageScale = (int) Math
						.floor((imageScaleWidth > imageScaleHeight ? imageScaleHeight : imageScaleWidth));

				// inSampleSizeには2のべき上が入るべきなので、imageScaleに最も近く、かつそれ以下の2のべき上の数を探す
				for (int i = 2; i <= imageScale; i *= 2) {
					imageOptions2.inSampleSize = i;
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				rotatedBitmap.compress(CompressFormat.JPEG, 100, baos);
				byte[] rot_bytes = baos.toByteArray();
				ByteArrayInputStream bis = new ByteArrayInputStream(rot_bytes);
				shrinked_bitmap = BitmapFactory.decodeStream(bis, null, imageOptions2);
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
				System.out.println("Sample Size: 1/" + imageOptions2.inSampleSize);				
			} else {
				shrinked_bitmap = rotatedBitmap;
			}
			// ---
                                                
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            shrinked_bitmap.compress(CompressFormat.JPEG, 100, baos);
            byte[] shrinked_data = baos.toByteArray();
            
            String saveDir = Environment.getExternalStorageDirectory().getPath() + "/GanmenScouter";
            // SD カードフォルダを取得
            File file = new File(saveDir);

            // フォルダ作成
            if (!file.exists()) {
                if (!file.mkdir()) {
                    System.out.println("error: mkdir failed");
                }
            }

            // 画像保存パス
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String imgPath = saveDir + "/" + sf.format(cal.getTime()) + ".jpg";

            // ファイル保存
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(imgPath, true);
                fos.write(shrinked_data);
                fos.close();

                // アンドロイドのデータベースへ登録
                // (登録しないとギャラリーなどにすぐに反映されないため)
                registAndroidDB(imgPath);

            } catch (Exception e) {
                System.out.println(e);
            }

            fos = null;

            double tmp = 0;
            if(!isMaleMode){
            	tmp = facePP.measure_similarity(facePP.get_face_id("japanese_bijin.png", getAssets()), facePP.get_face_id(shrinked_data));
            }else{
            	tmp = facePP.measure_similarity(facePP.get_face_id("otoko_ikemen.png", getAssets()), facePP.get_face_id(shrinked_data));
            }
            
            double result_val = 50 + tmp;
            result_val = Math.floor(result_val);
//            tv_top.setText(String.valueOf(result_val) + "点");
            
            mIsTake = false;
 
            // takePicture するとプレビューが停止するので、再度プレビュースタート
            if(mCam == null){
            	inCam.startPreview();
            }else{
            	mCam.startPreview();             	
            }
            
            try {
            	called_intent = true;
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_TEXT, "顔面偏差値 " + String.valueOf(result_val) + "点でした! http://bit.ly/1NbvhcO  #顔面スカウター");
                startActivityForResult(Intent.createChooser(intent, String.valueOf(result_val) + "点を共有"), 101);
            } catch (Exception e) {
            }

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
        }
    };
    
    /**
     * アンドロイドのデータベースへ画像のパスを登録
     * @param path 登録するパス
     */
    private void registAndroidDB(String path) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = GanmenScouter.this.getContentResolver();
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
    
	@Override
	protected void onResume() {
		super.onResume();
//		if(called_intent){
//			setup_cam_and_preview(false);
//			called_intent = false;
//		}
	    
		if(mCam == null){
			inCam.startPreview();
		}else{
			mCam.startPreview();		
		}
	}

    @Override
    protected void onPause() {
        super.onPause();    	
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }    

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// フォーカス変更後の処理

		rootView_ = mCamPreview;
		
		if(mCam != null){
//	        Camera.Parameters parameters = mCam.getParameters();

	        // 縦画面の場合回転させる
	        if ( rootView_.getWidth() < rootView_.getHeight()) {
	            // 縦画面
//	          parameters.setRotation(90);
	            mCam.setDisplayOrientation(90);
//	            mCam.setDisplayOrientation(0);
	        }else{
	            // 横画面
//	          parameters.setRotation(0);
	            mCam.setDisplayOrientation(90); //0
	        }				
		}	
		
		// ---
		// View作成

		boolean isLandscape = rootView_.getWidth() > rootView_.getHeight(); // 横画面か?

		ViewGroup.LayoutParams rtlp = rootView_.getLayoutParams();
		if (isLandscape) {
			// 横画面
			rtlp.width = rootView_.getHeight() * width / height;
			rtlp.height = rootView_.getHeight();
		} else {
			// 縦画面
			rtlp.width = rootView_.getWidth();
			if (width > height){
				rtlp.height = rootView_.getWidth() * width / height;
			}else{
				rtlp.height = rootView_.getWidth() * height / width;				
			}
		}
		rootView_.setLayoutParams(rtlp);

		// ---
	}
    

	
    private static int getCameraDisplayOrientation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }
        return (90 + 360 - degrees) % 360;
    }
    
}