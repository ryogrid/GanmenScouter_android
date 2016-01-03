package com.ganmen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class GanmenScouter extends Activity {
	private final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
	private final String API_KEY = "b090f5a8d9c833fa9ef8bc6d2bc4e647";
	private final String API_SECRET = "S0e2ZbFYUSOfXpyfou4B67H5Cklr_4di";

    // プレビューサイズ
    static private int width = -1;
    static private int height = -1;
    
	private Button button1;

	private EditText edit;
	private TextView tv_top;
	
    // カメラインスタンス
    private Camera mCam = null;

    // カメラプレビュークラス
    private CameraPreview mCamPreview = null;

    // 画面タッチの2度押し禁止用フラグ
    private boolean mIsTake = false;
    
    View rootView_ = null;
    
    Activity root_act = this; 
    
    boolean called_intent = false;
    
    @Override
    public void onStart() {
      super.onStart();
   // Obtain the shared Tracker instance.
      AnalyticsApplication.tracker().setScreenName("main screen");
      AnalyticsApplication.tracker().send(new HitBuilders.ScreenViewBuilder().build());      
    }    
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setup_cam_and_preview();
    }
	
	private void setup_cam_and_preview(){        
        // カメラインスタンスの取得
        try {
            mCam = Camera.open();
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


		// FrameLayout に CameraPreview クラスを設定
		FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
		
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
		tv_top.setText("ここに結果を出すよ");
		preview.addView(tv_top, LayoutParams.WRAP_CONTENT);
		
	}

    // AF完了時のコールバック
    private Camera.AutoFocusCallback autoFocusListener_ = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            camera.autoFocus(null);
            mCam.takePicture(null, null, mPicJpgListener);
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
            m.postRotate(degrees);
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

            double tmp = measure_similarity(get_face_id("japanese_bijin.png"), get_face_id(shrinked_data));
            double result_val = 50 + 2 * (tmp - 46);
            result_val = Math.floor(result_val);
            tv_top.setText(String.valueOf(result_val) + "点");
            
            mIsTake = false;
            
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
                startActivityForResult(Intent.createChooser(intent, String.valueOf(result_val) + "点を共有（しない場合は端末の戻るボタンを押して下さい）"), 101);
            } catch (Exception e) {
            }
            
            // takePicture するとプレビューが停止するので、再度プレビュースタート
            mCam.startPreview();             
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
		mCam.startPreview();		
	}

    @Override
    protected void onPause() {
        super.onPause();
//        // カメラ破棄インスタンスを解放
//        if (mCam != null) {
//            mCam.release();
//            mCam = null;
//        }
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
		JSONArray tmp = result.getJSONArray("face");
		if(tmp.length() > 0){
			ret = tmp.getJSONObject(0).getString("face_id");
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
			JSONArray tmp = result.getJSONArray("face");
			if(tmp != null){
				ret = tmp.getJSONObject(0).getString("face_id");
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
			 ret = result.getDouble("similarity");			 
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}
	
	private void measure_goodness() {		
		//tv_top.setText(get_face_id("japanese_bijin.png"));
		tv_top.setText(String.valueOf(measure_similarity(get_face_id("japanese_bijin.png"), get_face_id("i320.jpeg"))));
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