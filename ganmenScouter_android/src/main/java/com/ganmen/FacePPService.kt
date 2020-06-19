package com.ganmen

import android.content.res.AssetManager
import com.facepp.error.FaceppParseException
import com.facepp.http.HttpRequests
import com.facepp.http.PostParameters
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException

class FacePPService {
    private val API_KEY = "6w7AMUqM_MRztslYVnDGXso6zWPdNdLy"
    private val API_SECRET = "cPnh_soPUVuSjThWxBJZEse9ODkB8-IW"
    private fun thread_sleep(millis: Int) {
        try {
            Thread.sleep(millis.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun get_face_id(data: ByteArray): String {
        //HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
        val httpRequests = HttpRequests(API_KEY, API_SECRET, false, false)
        var result: JSONObject? = null
        try {
            //detect
            result = httpRequests.detectionDetect(PostParameters().setImg(data))
        } catch (e: FaceppParseException) {
            e.printStackTrace()
        }
        var ret: String = ""
        var tmp: JSONArray? = null
        try {
            tmp = result!!.getJSONArray("faces")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        if (tmp!!.length() > 0) {
            try {
                ret = tmp.getJSONObject(0).getString("face_token")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        //for avoiding api call frequency overs QPS limit of face++
        thread_sleep(1000)
        return ret
    }

    fun get_face_id(file_path: String, am: AssetManager): String {
        //HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
        val httpRequests = HttpRequests(API_KEY, API_SECRET, false, false)
        val b = ByteArray(1)
        //AssetManager am = getAssets();
        var fis: FileInputStream? = null
        try {
            val fd = am.openFd(file_path!!)
            fis = fd.createInputStream()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val baos = ByteArrayOutputStream()
        try {
            while (fis!!.read(b) > 0) {
                baos.write(b)
            }
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        try {
            baos.close()
            fis!!.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        val array = baos.toByteArray()
        var result: JSONObject? = null
        try {
            //detect
            result = httpRequests.detectionDetect(PostParameters().setImg(array))
        } catch (e: FaceppParseException) {
            e.printStackTrace()
        }
        var ret: String = ""
        //System.out.println(result);
        if (result != null) {
            var tmp: JSONArray? = null
            try {
                tmp = result.getJSONArray("faces")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (tmp != null) {
                try {
                    ret = tmp.getJSONObject(0).getString("face_token")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        //for avoiding api call frequency overs QPS limit of face++
        thread_sleep(1000)
        return ret
    }

    fun measure_similarity(face_id1: String, face_id2: String): Double {
        //HttpRequests httpRequests = new HttpRequests(API_KEY, API_SECRET, true, false);
        val httpRequests = HttpRequests(API_KEY, API_SECRET, false, false)
        val params = PostParameters()
        params.setFaceId1(face_id1)
        if (face_id2 == null) {
            return (-1).toDouble()
        }
        params.setFaceId2(face_id2)
        var result: JSONObject? = null
        var ret = -1.0
        try {
            result = httpRequests.recognitionCompare(params)
            ret = result.getDouble("confidence")
        } catch (e: FaceppParseException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return ret
    }
}