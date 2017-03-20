package com.ganmen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnDrawListener;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.facepp.*;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import com.ganmen.AnalyticsApplication;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class GanmenScouter extends Activity {
	private final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
	private final String API_KEY = "6w7AMUqM_MRztslYVnDGXso6zWPdNdLy";
	private final String API_SECRET = "cPnh_soPUVuSjThWxBJZEse9ODkB8-IW";

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
   // Obtain the shared Tracker instance.
      AnalyticsApplication.tracker().setScreenName("main screen");
      AnalyticsApplication.tracker().send(new HitBuilders.ScreenViewBuilder().build());
      
      Tracker t = AnalyticsApplication.tracker();
      // Enable Display Features.
      t.enableAdvertisingIdCollection(true);
    }    
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.main);        
        setup_cam_and_preview(true);
//		com.ad_stir.webview.AdstirMraidView adview = new com.ad_stir.webview.AdstirMraidView(
//			    this,
//			    "MEDIA-69c5161",
//			    2,
//			    com.ad_stir.webview.AdstirMraidView.AdSize.Size320x50,
//			    3);
//		adview.setBottom(500);
//		preview.addView(adview);        
		preview = (RelativeLayout) findViewById(R.id.cameraPreview);

		tv_top = new TextView(this);
		tv_top.setTextColor(Color.RED);
		tv_top.setText("　　　　　　　　　　　　　　　画面タッチで測定!");
		preview.addView(tv_top, LayoutParams.WRAP_CONTENT);
		
		MobileAds.initialize(getApplicationContext(), "ca-app-pub-3869533485696941/9899777318");
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

        requestNewInterstitial();
        
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        
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

		Button pselect_btn = (Button) findViewById(R.id.pselect_btn);
		pselect_btn.setText("撮影済画像を判定");
		pselect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_CHOOSER);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType("image/*");
//                startActivityForResult(intent, 1001);
                
//                Intent intentGallery;
//                intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
//                intentGallery.setType("image/*");
//                startActivityForResult(intentGallery, 1000);
                
                Intent i = new Intent( Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
            }
            
        });		
    }
	
	@Override protected void onActivityResult( int requestCode, int resultCode, Intent data) { 
		super.onActivityResult(requestCode, resultCode, data); 
		if (requestCode == 1 && resultCode == RESULT_OK && null != data) { 
			Uri selectedImage = data.getData(); 
			Bitmap bmap = null;
			try {
				bmap = getBitmapFromUri(selectedImage);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			PictureSelected(bmap);
//			String[] filePathColumn = { Media.DATA }; 
//			Cursor cursor = getContentResolver().query( selectedImage, filePathColumn, null, null, null); 
//			cursor.moveToFirst(); 
//			int columnIndex = cursor.getColumnIndex(filePathColumn[0]); 
//			String picturePath = cursor.getString(columnIndex); 
//			cursor.close();
		}
	}

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
    
    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                  .addTestDevice("SEE_YOUR_LOGCAT_TO_GET_YOUR_DEVICE_ID")
                  .build();

        mInterstitialAd.loadAd(adRequest);
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
    
    private void PictureSelected(Bitmap origBitmap){
		// もし、画像が大きかったら縮小して読み込む
		// 今回はimageSizeMaxの大きさに合わせる
		Bitmap shrinked_bitmap;
		int imageSizeMax = 240;
		float imageScaleWidth = origBitmap.getWidth() / imageSizeMax;
		float imageScaleHeight = origBitmap.getHeight() / imageSizeMax;

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
			origBitmap.compress(CompressFormat.JPEG, 100, baos);
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
			shrinked_bitmap = origBitmap;
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
        	tmp = measure_similarity(get_face_id("japanese_bijin.png"), get_face_id(shrinked_data));	
        }else{
        	tmp = measure_similarity(get_face_id("otoko_ikemen.png"), get_face_id(shrinked_data));            	
        }
        
        double result_val = 50 + tmp;
        result_val = Math.floor(result_val);

        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
        
		Button pselect_btn = (Button) findViewById(R.id.pselect_btn);
		pselect_btn.setText(result_val + "点！");
		
//        tv_top.setText("　　　　　　　　　　　　　　　" + result_val + "点！");
    }
    
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
            	tmp = measure_similarity(get_face_id("japanese_bijin.png"), get_face_id(shrinked_data));	
            }else{
            	tmp = measure_similarity(get_face_id("otoko_ikemen.png"), get_face_id(shrinked_data));            	
            }
            
            double result_val = 50 + 2 * (tmp - 46);
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
            	AnalyticsApplication.tracker().send(new HitBuilders.EventBuilder()
            		    .setCategory("Action")
            		    .setAction("TakePic")
            		    .build());
          
                
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
            
//            // takePicture するとプレビューが停止するので、再度プレビュースタート
//            if(mCam == null){
//            	inCam.startPreview();
//            }else{
//            	mCam.startPreview();             	
//            }
            
        }
    };

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data){
//        // takePicture するとプレビューが停止するので、再度プレビュースタート
//        mCam.startPreview();    	
//    }
    
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

		// // View内のView取得
		// SurfaceView surfaceView_ = (SurfaceView)
		// rootView.findViewById(R.id.surface_view);

		// // SurfaceHolder設定
		// SurfaceHolder holder = surfaceView_.getHolder();
		// holder.addCallback(surfaceListener_);
		// holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		//
		// // タッチリスナー設定
		// rootView_.setOnTouchListener(ontouchListener_);

		boolean isLandscape = rootView_.getWidth() > rootView_.getHeight(); // 横画面か?

		ViewGroup.LayoutParams rtlp = rootView_.getLayoutParams();
		if (isLandscape) {
			// 横画面
			rtlp.width = rootView_.getHeight() * width / height;
			//rtlp.width = rootView_.getHeight() * height / width;
			
			rtlp.height = rootView_.getHeight();
			//rtlp.height = rootView_.getHeight() * height / width;
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
    
    private String get_face_id(byte[] data){
    	HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
    	
    	JSONObject result = null;
		try {
			//detect
			 result = httpRequests.detectionDetect(new PostParameters().setImg(data));
		} catch (FaceppParseException e) {
			e.printStackTrace();
		}
		
		String ret = null;
		JSONArray tmp = result.getJSONArray("faces");
		if(tmp.length() > 0){
			ret = tmp.getJSONObject(0).getString("face_token");
		}

		return ret;    	
    }
	
	private String get_face_id(String file_path){
		HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
		
	    byte[] b = new byte[1];
	    AssetManager am = getAssets();	    
	    FileInputStream fis = null;
		try {
			AssetFileDescriptor fd = am.openFd(file_path);			
			fis = fd.createInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    try {
			while (fis.read(b) > 0) {
			    baos.write(b);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    try {
			baos.close();
		    fis.close();			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	    byte[] array = baos.toByteArray();		
		
	    JSONObject result = null;
		try {
			//detect
			 result = httpRequests.detectionDetect(new PostParameters().setImg(array));
		} catch (FaceppParseException e) {
			e.printStackTrace();
		}
		
		String ret = null;
		//System.out.println(result);
		if(result != null){
			JSONArray tmp = result.getJSONArray("faces");
			if(tmp != null){
				ret = tmp.getJSONObject(0).getString("face_token");
			}			
		}
		
		return ret;
	}
	
	private double measure_similarity(String face_id1, String face_id2){
		HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
		
		PostParameters params = new PostParameters();
		params.setFaceId1(face_id1);
		if(face_id2 == null){
			return -1;
		}
		params.setFaceId2(face_id2);

		JSONObject result = null;
	    double ret = -1;
		try {
			 result = httpRequests.recognitionCompare(params);
			 ret = result.getDouble("confidence");			 
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
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