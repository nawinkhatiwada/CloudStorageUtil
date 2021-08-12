package com.evolveasia.gCloudStorageUtils

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object Constants {

    const val BASE_URL = "https://www.googleapis.com/"
    val TYPE_JSON = "application/json; charset=UTF-8".toMediaTypeOrNull()
    const val DEFAULT_IMAGE_WIDTH = 500
    const val DEFAULT_IMAGE_HEIGHT = 500


}