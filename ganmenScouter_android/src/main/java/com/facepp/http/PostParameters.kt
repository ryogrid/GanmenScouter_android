package com.facepp.http

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Http Multipart<br></br>
 * `new PostParameters().setMode("oneface").setImg(new File("...")).setTag("some message")`
 * @author moon5kcq
 * @since 1.0.0
 * @version 1.3.0
 */
class PostParameters {
    var multipart: HashMap<String, ByteArray>
    private val boundary: String

    /**
     * auto generate boundary string
     * @return a boundary string
     */
    private fun getBoundary(): String {
        val sb = StringBuilder()
        val random = Random()
        for (i in 0 until boundaryLength) sb.append(boundaryAlphabet[random.nextInt(boundaryAlphabet.length)])
        return sb.toString()
    }

    /**
     * @return multipart boundary string
     */
    fun boundaryString(): String {
        return boundary
    }

    /**
     * async=true|false
     * @param flag
     * @return this
     */
    fun setAsync(flag: Boolean): PostParameters {
        addString("async", "" + flag)
        return this
    }

    /**
     * url=...
     * @param url
     * @return this
     */
    fun setUrl(url: String): PostParameters {
        addString("url", url)
        return this
    }

    /**
     * attribute = gender | age | race | all | none
     * @param type
     * @return this
     */
    fun setAttribute(type: String): PostParameters {
        addString("attribute", type)
        return this
    }

    /**
     * tag=...
     * @param tag
     * @return this
     */
    fun setTag(tag: String): PostParameters {
        addString("tag", tag)
        return this
    }

    /**
     * img=...
     * @param file
     * @return this
     */
    fun setImg(file: File): PostParameters {
        try {
            multipart!!["img"] = readFileToByte(file.path)
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return this
    }

    /**
     * img=...
     * @param data
     * @return this
     */
    fun setImg(data: ByteArray): PostParameters {
        setImg(data, "NoName")
        return this
    }

    /**
     * img=...(name in multipart is ...)
     * @param data
     * @param fileName
     * @return this
     */
    fun setImg(data: ByteArray, fileName: String): PostParameters {
        if (fileName == "NoName") {
            multipart!!["image_file"] = data
        } else {
            try {
                multipart!!["img"] = readFileToByte(fileName)
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        return this
    }

    /**
     * face_id1=...
     * @param id
     * @return this
     */
    fun setFaceId1(id: String): PostParameters {
        addString("face_id1", id)
        return this
    }

    /**
     * face_id2=...
     * @param id
     * @return this
     */
    fun setFaceId2(id: String): PostParameters {
        addString("face_id2", id)
        return this
    }

    /**
     * group_name=...
     * @param groupName
     * @return this
     */
    fun setGroupName(groupName: String): PostParameters {
        addString("group_name", groupName)
        return this
    }

    /**
     * group_id=...
     * @param groupId
     * @return this
     */
    fun setGroupId(groupId: String): PostParameters {
        addString("group_id", groupId)
        return this
    }

    /**
     * key_face_id=...
     * @param id
     * @return this
     */
    fun setKeyFaceId(id: String): PostParameters {
        addString("key_face_id", id)
        return this
    }

    /**
     * count=...
     * @param count
     * @return this
     */
    fun setCount(count: Int): PostParameters {
        addString("count", count.toString())
        return this
    }

    /**
     * type= all | search | recognize
     * @param type
     * @return this
     */
    fun setType(type: String): PostParameters {
        addString("type", type)
        return this
    }

    /**
     * face_id=...
     * @param faceId
     * @return this
     */
    fun setFaceId(faceId: String): PostParameters {
        addString("face_id", faceId)
        return this
    }

    /**
     * faceset_id=...
     * @param facesetId
     * @return this
     */
    fun setFacesetId(facesetId: String): PostParameters {
        addString("faceset_id", facesetId)
        return this
    }

    /**
     * faceset_id=..., ..., ...
     * @param facesetIds
     * @return this
     */
    fun setFacesetId(facesetId: Array<String>): PostParameters {
        setFacesetId(toStringList(facesetId))
        return this
    }

    /**
     * faceset_id=...
     * @param facesetIds
     * @return this
     */
    fun setFacesetId(facesetId: ArrayList<String>): PostParameters {
        setFacesetId(toStringList(facesetId))
        return this
    }

    /**
     * person_id=...
     * @param personId
     * @return this
     */
    fun setPersonId(personId: String): PostParameters {
        addString("person_id", personId)
        return this
    }

    /**
     * person_name=...
     * @param personName
     * @return this
     */
    fun setPersonName(personName: String): PostParameters {
        addString("person_name", personName)
        return this
    }

    /**
     * name=...
     * @param name
     * @return this
     */
    fun setName(name: String): PostParameters {
        addString("name", name)
        return this
    }

    /**
     * session_id=...
     * @param id
     * @return this
     */
    fun setSessionId(id: String): PostParameters {
        addString("session_id", id)
        return this
    }

    /**
     * mode= oneface | normal
     * @param type
     * @return this
     */
    fun setMode(type: String): PostParameters {
        addString("mode", type)
        return this
    }

    /**
     * face_id=... , ... , ...
     * @param faceIds
     * @return this
     */
    fun setFaceId(faceIds: Array<String>): PostParameters {
        return setFaceId(toStringList(faceIds))
    }

    /**
     * person_id=... , ... , ...
     * @param personIds
     * @return this
     */
    fun setPersonId(personIds: Array<String>): PostParameters {
        return setPersonId(toStringList(personIds))
    }

    /**
     * person_name=... , ... , ...
     * @param personNames
     * @return this
     */
    fun setPersonName(personNames: Array<String>): PostParameters {
        return setPersonName(toStringList(personNames))
    }

    /**
     * group_id=... , ... , ...
     * @param groupIds
     * @return this
     */
    fun setGroupId(groupIds: Array<String>): PostParameters {
        return setGroupId(toStringList(groupIds))
    }

    /**
     * group_name=... , ... , ...
     * @param groupNames
     * @return this
     */
    fun setGroupName(groupNames: Array<String>): PostParameters {
        return setGroupName(toStringList(groupNames))
    }

    /**
     * face=... , ... , ...
     * @param faceIds
     * @return this
     */
    fun setFaceId(faceIds: ArrayList<String>): PostParameters {
        return setFaceId(toStringList(faceIds))
    }

    /**
     * person_id=... , ... , ...
     * @param personIds
     * @return this
     */
    fun setPersonId(personIds: ArrayList<String>): PostParameters {
        return setPersonId(toStringList(personIds))
    }

    /**
     * person_name=... , ... , ...
     * @param personNames
     * @return this
     */
    fun setPersonName(personNames: ArrayList<String>): PostParameters {
        return setPersonName(toStringList(personNames))
    }

    /**
     * group_id=... , ... , ...
     * @param groupIds
     * @return this
     */
    fun setGroupId(groupIds: ArrayList<String>): PostParameters {
        return setGroupId(toStringList(groupIds))
    }

    /**
     * group_name=... , ... , ...
     * @param groupNames
     * @return this
     */
    fun setGroupName(groupNames: ArrayList<String>): PostParameters {
        return setGroupName(toStringList(groupNames))
    }

    /**
     * img_id=...
     * @param imgId
     * @return this
     */
    fun setImgId(imgId: String): PostParameters {
        addString("img_id", imgId)
        return this
    }

    /**
     * faceset_name=...
     * @param facesetName
     * @return this
     */
    fun setFacesetName(facesetName: String): PostParameters {
        addString("faceset_name", facesetName)
        return this
    }

    /**
     * faceset_name=... , ... , ...
     * @param facesetNames
     * @return this
     */
    fun setFacesetName(facesetNames: ArrayList<String>): PostParameters {
        return setFacesetName(toStringList(facesetNames))
    }

    /**
     * faceset_name=... , ... , ...
     * @param facesetNames
     * @return this
     */
    fun setFacesetName(facesetNames: Array<String>): PostParameters {
        return setFacesetName(toStringList(facesetNames))
    }

    /**
     * `attr`=`value`
     * @param attr value
     * @return this
     */
    fun addAttribute(attr: String, value: String): PostParameters {
        addString(attr, value)
        return this
    }

    fun addString(id: String, str: String) {
        multipart!![id] = str.toByteArray()
    }

    private fun toStringList(sa: Array<String>): String {
        val sb = StringBuilder()
        for (i in sa.indices) {
            if (i != 0) sb.append(',')
            sb.append(sa[i])
        }
        return sb.toString()
    }

    private fun toStringList(sa: ArrayList<String>): String {
        val sb = StringBuilder()
        for (i in sa.indices) {
            if (i != 0) sb.append(',')
            sb.append(sa[i])
        }
        return sb.toString()
    }

    @Throws(Exception::class)
    private fun readFileToByte(filePath: String): ByteArray {
        var b = ByteArray(1)
        val fis = FileInputStream(filePath)
        val baos = ByteArrayOutputStream()
        while (fis.read(b) > 0) {
            baos.write(b)
        }
        baos.close()
        fis.close()
        b = baos.toByteArray()
        return b
    }

    val imgLength: String
        get() = multipart!!["img"]!!.size.toString()

    fun get_face_id1(): String {
        // TODO Auto-generated method stub
        return String(multipart!!["face_id1"]!!)
    }

    fun get_face_id2(): String {
        // TODO Auto-generated method stub
        return String(multipart!!["face_id2"]!!)
    }

    companion object {
        private const val boundaryLength = 32
        private const val boundaryAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_"
    }

    /**
     * default boundary is auto generate [.getBoundary]
     */
    init {
        boundary = getBoundary()
        //multiPart = new MultipartEntity(HttpMultipartMode.STRICT , boundary,  Charset.forName("UTF-8"));
        multipart = HashMap()
    }
}