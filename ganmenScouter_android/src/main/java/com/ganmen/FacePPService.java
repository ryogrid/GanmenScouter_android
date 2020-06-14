package com.ganmen;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class FacePPService {
    private final String API_KEY = "6w7AMUqM_MRztslYVnDGXso6zWPdNdLy";
    private final String API_SECRET = "cPnh_soPUVuSjThWxBJZEse9ODkB8-IW";

    public String get_face_id(byte[] data){
        HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);

        JSONObject result = null;
        try {
            //detect
            result = httpRequests.detectionDetect(new PostParameters().setImg(data));
        } catch (FaceppParseException e) {
            e.printStackTrace();
        }

        String ret = null;
        JSONArray tmp = null;
        try {
            tmp = result.getJSONArray("faces");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(tmp.length() > 0){
            try {
                ret = tmp.getJSONObject(0).getString("face_token");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    public String get_face_id(String file_path, AssetManager am){
        HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);

        byte[] b = new byte[1];
        //AssetManager am = getAssets();
        FileInputStream fis = null;
        try {
            AssetFileDescriptor fd = am.openFd(file_path);
            fis = fd.createInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (fis.read(b) > 0) {
                baos.write(b);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            baos.close();
            fis.close();
        } catch (IOException e1) {
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
            JSONArray tmp = null;
            try {
                tmp = result.getJSONArray("faces");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(tmp != null){
                try {
                    ret = tmp.getJSONObject(0).getString("face_token");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }

    public double measure_similarity(String face_id1, String face_id2){
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
        } catch (FaceppParseException | JSONException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
