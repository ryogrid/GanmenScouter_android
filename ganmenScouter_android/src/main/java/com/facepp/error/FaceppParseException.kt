package com.facepp.error

/**
 * exception about faceplusplus sdk
 * @author moon5ckq
 * @since 1.0.0
 * @version 1.0.0
 */
class FaceppParseException : Exception {
    var errorMessage: String? = null
        private set
    var errorCode: Int? = null
        private set
    var httpResponseCode: Int? = null
        private set

    constructor(message: String?) : super(message) {}
    constructor(message: String, errorCode: Int, errorMessage: String, httpResponseCode: Int) : super("$message code=$errorCode, message=$errorMessage, responseCode=$httpResponseCode") {
        this.errorCode = errorCode
        this.errorMessage = errorMessage
        this.httpResponseCode = httpResponseCode
    }

    val isAPIError: Boolean
        get() = errorCode != null && errorMessage != null && httpResponseCode != null

    companion object {
        private const val serialVersionUID: Long = 3
    }
}