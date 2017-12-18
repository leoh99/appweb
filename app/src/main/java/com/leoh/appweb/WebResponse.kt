package com.leoh.appweb

import java.io.Serializable
/**
 * Created by leoh on 12/14/17.
 */
data class WebResponse(val url: String, var mime: String, var encode: String, var data: ByteArray) : Serializable