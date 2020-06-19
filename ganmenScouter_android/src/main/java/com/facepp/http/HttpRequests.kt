package com.facepp.http

import com.facepp.error.FaceppParseException
import org.json.JSONException
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.util.*

//import org.apache.http.HttpConnection;
/**
 * request to faceplusplus.com<br></br>
 * `new HttpRequests(apiKey, apiSecret).request("detection", "detect", postParameters)`<br></br>
 *
 * `new HttpRequests(apiKey, apiSecret).train()`
 * @author moon5ckq
 * @since 1.0.0
 * @version 1.3.0
 */
class HttpRequests {
    private var webSite: String? = null
    /**
     * @return api_key
     */
    /**
     * @param apiKey
     */
    var apiKey: String? = null
    /**
     * @return api_secret
     */
    /**
     * @param apiSecret
     */
    var apiSecret: String? = null
    private var params: PostParameters? = null
    /**
     * (million second)
     * @return http timeout limit
     */
    /**
     * default is 30 sec
     * set http timeout limit (million second)
     * @param timeOut
     */
    var httpTimeOut = TIMEOUT

    /**
     * if isCN is true, then use AliCloud, false to Amazon<br></br>
     * if isDebug is true, then use http, otherwise https
     * @param isCN
     * @param isDebug
     */
    fun setWebSite(isCN: Boolean, isDebug: Boolean) {
        if (isCN && isDebug) webSite = DWEBSITE_CN else if (isCN && !isDebug) webSite = WEBSITE_CN else if (!isCN && isDebug) webSite = DWEBSITE_US else if (!isCN && !isDebug) webSite = WEBSITE_US
    }

    /**
     * @return a webSite clone
     */
    fun getWebSite(): String? {
        return webSite
    }

    /**
     * default timeout time is 1 minute
     * @param sessionId
     * @return the getSession Result
     * @throws FaceppParseException
     */
    @Throws(FaceppParseException::class)
    fun getSessionSync(sessionId: String): JSONObject? {
        return getSessionSync(sessionId, SYNC_TIMEOUT.toLong())
    }

    /**
     * timeout time is [timeOut]ms, the method is synchronized.
     * @param sessionId
     * @param timeOut
     * @return the getSession Result
     * @throws FaceppParseException
     */
    @Throws(FaceppParseException::class)
    fun getSessionSync(sessionId: String, timeOut: Long): JSONObject? {
        val sb = StringBuilder()
        val t = Date().time + timeOut
        while (true) {
            val rst = request("info", "get_session", PostParameters().setSessionId(sessionId))
            try {
                if (rst.getString("status") == "SUCC") {
                    sb.append(rst.toString())
                    break
                } else if (rst.getString("status") == "INVALID_SESSION") {
                    sb.append("INVALID_SESSION")
                    break
                }
            } catch (e: JSONException) {
                sb.append("Unknow error.")
                break
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                sb.append("Thread.sleep error.")
                break
            }
            if (Date().time >= t) {
                sb.append("Time Out")
                break
            }
        }
        val rst = sb.toString()
        if (rst == "INVALID_SESSION") {
            throw FaceppParseException("Invaild session, unknow error.")
        } else if (rst == "Unknow error.") {
            throw FaceppParseException("Unknow error.")
        } else if (rst == "Thread.sleep error.") {
            throw FaceppParseException("Thread.sleep error.")
        } else if (rst == "Time Out") {
            throw FaceppParseException("Get session time out.")
        } else {
            try {
                val result = JSONObject(rst)
                result.put("response_code", 200)
                return result
            } catch (e: JSONException) {
            }
        }
        return null
    }

    /**
     * [.request]<br></br>
     * faceplusplus.com/[control]/[action]<br></br>
     * default use parameters which [.getParams]
     * @param control
     * @param action
     * @return a result object
     */
    @JvmOverloads
    @Throws(FaceppParseException::class)
    fun request(control: String?, action: String, params: PostParameters = getParams()): JSONObject {
        val url: URL
        var urlConn: HttpURLConnection? = null
        return try {
            //url = new URL(webSite+control+"/"+action);
            url = if (action == "compare") {
                URL(webSite + action + "?api_key=" + apiKey + "&api_secret=" + apiSecret + "&face_token1=" + params.get_face_id1() + "&face_token2=" + params.get_face_id2())
            } else { //detection
                URL("$webSite$action?api_key=$apiKey&api_secret=$apiSecret")
            }
            urlConn = url.openConnection() as HttpURLConnection
            urlConn = if (action == "compare") {
                request_compare(urlConn, params)
            } else { //detection
                request_detection(urlConn, params)
            }
            var resultString: String? = null
            resultString = if (urlConn!!.responseCode == 200) readString(urlConn.inputStream) else readString(urlConn.errorStream)
            println(resultString)
            //FaceppResult result = new FaceppResult( new JSONObject(resultString), urlConn.getResponseCode());
            val result = JSONObject(resultString)
            if (result.has("error")) {
                if (result.getString("error") == "API not found") throw FaceppParseException("API not found")
                throw FaceppParseException("API error.", result.getInt("error_code"),
                        result.getString("error"), urlConn.responseCode)
            }
            result.put("response_code", urlConn.responseCode)
            urlConn.inputStream.close()
            result
        } catch (e: Exception) {
            throw FaceppParseException("error :$e")
        } finally {
            urlConn?.disconnect()
        }
    }

    private fun request_detection(urlConn: HttpURLConnection?, params: PostParameters): HttpURLConnection? {
        try {
            urlConn!!.requestMethod = "POST"
        } catch (e: ProtocolException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        urlConn!!.connectTimeout = httpTimeOut
        urlConn.readTimeout = httpTimeOut
        //urlConn.setChunkedStreamingMode(0);
        urlConn.doOutput = true
        urlConn.setRequestProperty("connection", "keep-alive")
        urlConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + params.boundaryString())
        val twoHyphens = "--"
        //final String boundary =  "*****"+ UUID.randomUUID().toString()+"*****";
        val boundary = params.boundaryString()
        val lineEnd = "\r\n"
        val maxBufferSize = 1024 * 1024 * 3
        val contentsBuilder = StringBuilder()
        var closingContents = ""
        var iContentsLength = 0
        var file_data: ByteArray? = null
        val outputStream: DataOutputStream
        try {
            val multiPart = params.multipart
            contentsBuilder.append(lineEnd)
            for ((key, value) in multiPart) {
                if (key == "image_file") {
                    contentsBuilder.append(twoHyphens + boundary + lineEnd)
                    contentsBuilder.append("Content-Disposition: form-data;name=\"$key\";filename=\"hoge.png\"$lineEnd")
                    contentsBuilder.append("Content-Type: application/octet-stream$lineEnd")
                    contentsBuilder.append(lineEnd)
                    file_data = value
                }
            }
            closingContents = lineEnd + twoHyphens + boundary + twoHyphens + lineEnd
            iContentsLength += contentsBuilder.toString().toByteArray(charset("UTF-8")).size
            iContentsLength += closingContents.toByteArray(charset("UTF-8")).size
            iContentsLength += file_data!!.size
            urlConn.setRequestProperty("Content-Length", iContentsLength.toString())
            outputStream = DataOutputStream(urlConn.outputStream)
            outputStream.writeBytes(contentsBuilder.toString())
            outputStream.write(file_data)
            outputStream.writeBytes(closingContents)
            outputStream.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        //params.sendMultipart(new DataOutputStream(urlConn.getOutputStream()));
        return urlConn
    }

    private fun request_compare(urlConn: HttpURLConnection?, params: PostParameters): HttpURLConnection? {
        try {
            urlConn!!.requestMethod = "GET"
        } catch (e: ProtocolException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        urlConn!!.connectTimeout = httpTimeOut
        urlConn.readTimeout = httpTimeOut
        urlConn.doOutput = true
        urlConn.setRequestProperty("connection", "keep-alive")
        return urlConn
    }

    /**
     * create [HttpRequests] <br></br>
     * api_key=...,api_secret=... <br></br>
     * use https and AliCloud default
     * @param apiKey
     * @param apiSecret
     */
    constructor(apiKey: String?, apiSecret: String?) : super() {
        this.apiKey = apiKey
        this.apiSecret = apiSecret
        webSite = WEBSITE_CN
    }

    /**
     * use https default
     * create a empty [HttpRequests] object
     */
    constructor() : super() {}

    /**
     * create [HttpRequests] <br></br>
     * api_key=...,api_secret=...<br></br>
     * the isCN and isDebug use like
     * @param apiKey
     * @param apiSecret
     * @param isCN
     * @param isDebug
     */
    constructor(apiKey: String?, apiSecret: String?, isCN: Boolean, isDebug: Boolean) : super() {
        this.apiKey = apiKey
        this.apiSecret = apiSecret
        setWebSite(isCN, isDebug)
    }

    /**
     * @return [PostParameters] object
     */
    fun getParams(): PostParameters {
        if (params == null) params = PostParameters()
        return params!!
    }

    /**
     * set default PostParameters
     * @param params
     */
    fun setParams(params: PostParameters?) {
        this.params = params
    }
    /**
     * used by offline detect
     * example: request.offlineDetect(detecter.getImageByteArray(), detecter.getResultJsonString(), params);
     * @param image
     * @param jsonResult
     * @param params
     * @return
     * @throws FaceppParseException
     */
    /**
     * used by offline detect
     * example: request.offlineDetect(detecter.getImageByteArray(), detecter.getResultJsonString());
     * @param image
     * @param jsonResult
     * @return
     * @throws FaceppParseException
     */
    @JvmOverloads
    @Throws(FaceppParseException::class)
    fun offlineDetect(image: ByteArray, jsonResult: String, params: PostParameters? = this.params): JSONObject {
        var params = params
        if (params == null) params = PostParameters()
        params.setImg(image)
        params.setMode("offline")
        params.addAttribute("offline_result", jsonResult)
        return request("detection", "detect", params)
    }

    //all api here
    @Throws(FaceppParseException::class)
    fun detectionDetect(): JSONObject {
        return request("detection", "detect")
    }

    @Throws(FaceppParseException::class)
    fun detectionDetect(params: PostParameters): JSONObject {
        return request("detection", "detect", params)
    }

    @Throws(FaceppParseException::class)
    fun detectionLandmark(): JSONObject {
        return request("detection", "landmark")
    }

    @Throws(FaceppParseException::class)
    fun detectionLandmark(params: PostParameters): JSONObject {
        return request("detection", "landmark", params)
    }

    @Throws(FaceppParseException::class)
    fun trainVerify(): JSONObject {
        return request("train", "verify")
    }

    @Throws(FaceppParseException::class)
    fun trainVerify(params: PostParameters): JSONObject {
        return request("train", "verify", params)
    }

    @Throws(FaceppParseException::class)
    fun trainSearch(): JSONObject {
        return request("train", "search")
    }

    @Throws(FaceppParseException::class)
    fun trainSearch(params: PostParameters): JSONObject {
        return request("train", "search", params)
    }

    @Throws(FaceppParseException::class)
    fun trainIdentify(): JSONObject {
        return request("train", "identify")
    }

    @Throws(FaceppParseException::class)
    fun trainIdentify(params: PostParameters): JSONObject {
        return request("train", "identify", params)
    }

    @Throws(FaceppParseException::class)
    fun recognitionCompare(): JSONObject {
        return request("recognition", "compare")
    }

    @Throws(FaceppParseException::class)
    fun recognitionCompare(params: PostParameters): JSONObject {
        return request("recognition", "compare", params)
    }

    @Throws(FaceppParseException::class)
    fun recognitionVerify(): JSONObject {
        return request("recognition", "verify")
    }

    @Throws(FaceppParseException::class)
    fun recognitionVerify(params: PostParameters): JSONObject {
        return request("recognition", "verify", params)
    }

    @Throws(FaceppParseException::class)
    fun recognitionSearch(): JSONObject {
        return request("recognition", "search")
    }

    @Throws(FaceppParseException::class)
    fun recognitionSearch(params: PostParameters): JSONObject {
        return request("recognition", "search", params)
    }

    @Throws(FaceppParseException::class)
    fun recognitionIdentify(): JSONObject {
        return request("recognition", "identify")
    }

    @Throws(FaceppParseException::class)
    fun recognitionIdentify(params: PostParameters): JSONObject {
        return request("recognition", "identify", params)
    }

    @Throws(FaceppParseException::class)
    fun groupingGrouping(): JSONObject {
        return request("grouping", "grouping")
    }

    @Throws(FaceppParseException::class)
    fun groupingGrouping(params: PostParameters): JSONObject {
        return request("grouping", "grouping", params)
    }

    @Throws(FaceppParseException::class)
    fun personCreate(): JSONObject {
        return request("person", "create")
    }

    @Throws(FaceppParseException::class)
    fun personCreate(params: PostParameters): JSONObject {
        return request("person", "create", params)
    }

    @Throws(FaceppParseException::class)
    fun personDelete(): JSONObject {
        return request("person", "delete")
    }

    @Throws(FaceppParseException::class)
    fun personDelete(params: PostParameters): JSONObject {
        return request("person", "delete", params)
    }

    @Throws(FaceppParseException::class)
    fun personAddFace(): JSONObject {
        return request("person", "add_face")
    }

    @Throws(FaceppParseException::class)
    fun personAddFace(params: PostParameters): JSONObject {
        return request("person", "add_face", params)
    }

    @Throws(FaceppParseException::class)
    fun personRemoveFace(): JSONObject {
        return request("person", "remove_face")
    }

    @Throws(FaceppParseException::class)
    fun personRemoveFace(params: PostParameters): JSONObject {
        return request("person", "remove_face", params)
    }

    @Throws(FaceppParseException::class)
    fun personSetInfo(): JSONObject {
        return request("person", "set_info")
    }

    @Throws(FaceppParseException::class)
    fun personSetInfo(params: PostParameters): JSONObject {
        return request("person", "set_info", params)
    }

    @Throws(FaceppParseException::class)
    fun personGetInfo(): JSONObject {
        return request("person", "get_info")
    }

    @Throws(FaceppParseException::class)
    fun personGetInfo(params: PostParameters): JSONObject {
        return request("person", "get_info", params)
    }

    @Throws(FaceppParseException::class)
    fun facesetCreate(): JSONObject {
        return request("faceset", "create")
    }

    @Throws(FaceppParseException::class)
    fun facesetCreate(params: PostParameters): JSONObject {
        return request("faceset", "create", params)
    }

    @Throws(FaceppParseException::class)
    fun facesetDelete(): JSONObject {
        return request("faceset", "delete")
    }

    @Throws(FaceppParseException::class)
    fun facesetDelete(params: PostParameters): JSONObject {
        return request("faceset", "delete", params)
    }

    @Throws(FaceppParseException::class)
    fun facesetAddFace(): JSONObject {
        return request("faceset", "add_face")
    }

    @Throws(FaceppParseException::class)
    fun facesetAddFace(params: PostParameters): JSONObject {
        return request("faceset", "add_face", params)
    }

    @Throws(FaceppParseException::class)
    fun facesetRemoveFace(): JSONObject {
        return request("faceset", "remove_face")
    }

    @Throws(FaceppParseException::class)
    fun facesetRemoveFace(params: PostParameters): JSONObject {
        return request("faceset", "remove_face", params)
    }

    @Throws(FaceppParseException::class)
    fun facesetSetInfo(): JSONObject {
        return request("faceset", "set_info")
    }

    @Throws(FaceppParseException::class)
    fun facesetSetInfo(params: PostParameters): JSONObject {
        return request("faceset", "set_info", params)
    }

    @Throws(FaceppParseException::class)
    fun facesetGetInfo(): JSONObject {
        return request("faceset", "get_info")
    }

    @Throws(FaceppParseException::class)
    fun facesetGetInfo(params: PostParameters): JSONObject {
        return request("faceset", "get_info", params)
    }

    @Throws(FaceppParseException::class)
    fun groupCreate(): JSONObject {
        return request("group", "create")
    }

    @Throws(FaceppParseException::class)
    fun groupCreate(params: PostParameters): JSONObject {
        return request("group", "create", params)
    }

    @Throws(FaceppParseException::class)
    fun groupDelete(): JSONObject {
        return request("group", "delete")
    }

    @Throws(FaceppParseException::class)
    fun groupDelete(params: PostParameters): JSONObject {
        return request("group", "delete", params)
    }

    @Throws(FaceppParseException::class)
    fun groupAddPerson(): JSONObject {
        return request("group", "add_person")
    }

    @Throws(FaceppParseException::class)
    fun groupAddPerson(params: PostParameters): JSONObject {
        return request("group", "add_person", params)
    }

    @Throws(FaceppParseException::class)
    fun groupRemovePerson(): JSONObject {
        return request("group", "remove_person")
    }

    @Throws(FaceppParseException::class)
    fun groupRemovePerson(params: PostParameters): JSONObject {
        return request("group", "remove_person", params)
    }

    @Throws(FaceppParseException::class)
    fun groupSetInfo(): JSONObject {
        return request("group", "set_info")
    }

    @Throws(FaceppParseException::class)
    fun groupSetInfo(params: PostParameters): JSONObject {
        return request("group", "set_info", params)
    }

    @Throws(FaceppParseException::class)
    fun groupGetInfo(): JSONObject {
        return request("group", "get_info")
    }

    @Throws(FaceppParseException::class)
    fun groupGetInfo(params: PostParameters): JSONObject {
        return request("group", "get_info", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetImage(): JSONObject {
        return request("info", "get_image")
    }

    @Throws(FaceppParseException::class)
    fun infoGetImage(params: PostParameters): JSONObject {
        return request("info", "get_image", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetFace(): JSONObject {
        return request("info", "get_face")
    }

    @Throws(FaceppParseException::class)
    fun infoGetFace(params: PostParameters): JSONObject {
        return request("info", "get_face", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetPersonList(): JSONObject {
        return request("info", "get_person_list")
    }

    @Throws(FaceppParseException::class)
    fun infoGetPersonList(params: PostParameters): JSONObject {
        return request("info", "get_person_list", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetFacesetList(): JSONObject {
        return request("info", "get_faceset_list")
    }

    @Throws(FaceppParseException::class)
    fun infoGetFacesetList(params: PostParameters): JSONObject {
        return request("info", "get_faceset_list", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetGroupList(): JSONObject {
        return request("info", "get_group_list")
    }

    @Throws(FaceppParseException::class)
    fun infoGetGroupList(params: PostParameters): JSONObject {
        return request("info", "get_group_list", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetSession(): JSONObject {
        return request("info", "get_session")
    }

    @Throws(FaceppParseException::class)
    fun infoGetSession(params: PostParameters): JSONObject {
        return request("info", "get_session", params)
    }

    /**
     * @return
     * @throws FaceppParseException
     */
    @Deprecated("""this api is deprecated
	  """)
    @Throws(FaceppParseException::class)
    fun infoGetQuota(): JSONObject {
        return request("info", "get_quota")
    }

    /**
     * @return
     * @throws FaceppParseException
     */
    @Deprecated("""this api is deprecated
	  """)
    @Throws(FaceppParseException::class)
    fun infoGetQuota(params: PostParameters): JSONObject {
        return request("info", "get_quota", params)
    }

    @Throws(FaceppParseException::class)
    fun infoGetApp(): JSONObject {
        return request("info", "get_app")
    }

    @Throws(FaceppParseException::class)
    fun infoGetApp(params: PostParameters): JSONObject {
        return request("info", "get_app", params)
    }

    companion object {
        private const val WEBSITE_CN = "https://api-us.faceplusplus.com/facepp/v3/"
        private const val DWEBSITE_CN = "https://api-us.faceplusplus.com/facepp/v3/"
        private const val WEBSITE_US = "https://api-us.faceplusplus.com/facepp/v3/"
        private const val DWEBSITE_US = "https://api-us.faceplusplus.com/facepp/v3/"
        private const val BUFFERSIZE = 1048576
        private const val TIMEOUT = 30000
        private const val SYNC_TIMEOUT = 60000
        private fun readString(`is`: InputStream): String {
            val rst = StringBuffer()
            val buffer = ByteArray(BUFFERSIZE)
            var len = 0
            try {
                while (`is`.read(buffer).also { len = it } > 0) for (i in 0 until len) rst.append(buffer[i].toChar())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return rst.toString()
        }
    }
}