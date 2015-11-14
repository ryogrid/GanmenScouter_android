package com.ganmen;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.EditText;

import com.facepp.*;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.facepp.result.FaceppResult;

public class GanmenScouter extends Activity {
	private final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
	private final String API_KEY = "0e5ac228d92bc2c63c11c9aa47752b2a";
	private final String API_SECRET = "l7PsiUEj1TuF2b5_p369Ai8W6y_BnIsV";

	private Button button1;

	private EditText edit;
	private TextView tv_top;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		setContentView(linearLayout);

		tv_top = new TextView(this);
		tv_top.setText("番号を入力&距離(メートル)選択して投げてみよう");
		linearLayout.addView(tv_top, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

		TextView tv = new TextView(this);
		tv.setText("共通の番号を入れてね");
		linearLayout.addView(tv, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

		edit = new EditText(this);
		edit.setWidth(200);
		edit.setText("ここに入力してね");
		linearLayout.addView(edit, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
	
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 
        // アイテムを追加します      
        adapter.add("1");
        adapter.add("3");
        adapter.add("5");
        adapter.add("10");
        adapter.add("30");
        adapter.add("100");
 
        Spinner spinner = new Spinner(this);
        // アダプターを設定します
        spinner.setAdapter(adapter);
        // スピナーのアイテムが選択された時に呼び出されるコールバックリスナーを登録します
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View view,
                    int position, long id) {
                Spinner spinner = (Spinner) parent;
                // 選択されたアイテムを取得します
                String item = (String) spinner.getSelectedItem();
                //between = Integer.parseInt(item);
            }
            @Override
            public void onNothingSelected(AdapterView arg0) {
            }
        });
        linearLayout.addView(spinner, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));                

		button1 = new Button(this);
		button1.setText("類似度測定テスト");
		button1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//side = RECV;
				measure_goodness();
			}
		});
		linearLayout.addView(button1, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
    }		

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	private String get_face_id(String file_path){
		HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET);
		
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
		
	    FaceppResult result = null;
		try {
			//detect
			 result = httpRequests.detectionDetect(new PostParameters().setImg(array));
		} catch (FaceppParseException e) {
			e.printStackTrace();
		}
		
		String ret = null;
		try {
			ret = result.get("face").get(0).get("face_id").toString();
		} catch (FaceppParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	private double measure_similarity(String face_id1, String face_id2){
		HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET);
		
		PostParameters params = new PostParameters();
		params.setFaceId1(face_id1);
		params.setFaceId2(face_id2);

	    FaceppResult result = null;
	    double ret = -1;
		try {
			 result = httpRequests.recognitionCompare(params);
			 ret = result.get("similarity").toDouble();			 
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
}