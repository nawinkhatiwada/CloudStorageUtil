package com.evolveasia.gCloudStorageUtils

import android.graphics.Bitmap
import java.io.InputStream
import kotlin.properties.Delegates

class GCSMetaInfo(val bucketName: String,
                  var gcsStoragePath: String,
                  val imageMetaInfo: ImageMetaInfo,
                  val serviceConfig: InputStream) {

    private constructor(builder: Builder) : this(builder.bucketName, builder.gcsStoragePath, builder.imageMetaInfo, builder.serviceConfig)

    class Builder {
        var bucketName: String by Delegates.notNull()
        var imageMetaInfo: ImageMetaInfo by Delegates.notNull()
        var gcsStoragePath: String by Delegates.notNull()
        var serviceConfig: InputStream by Delegates.notNull()
        fun build() = GCSMetaInfo(this)
    }

    class ImageMetaInfo {
        var imagePath: String by Delegates.notNull()
        var mediaType: String by Delegates.notNull()
        var imageByteArray: ByteArray by Delegates.notNull()
        var metadata: String by Delegates.notNull()
        var compressLevel: Int = 100
        var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
        var imageWidth: Int? = null
        var imageHeight: Int? = null
        var waterMarkInfo: WaterMarkInfo? = null


        companion object {
            val TYPE_JPEG = "image/jpeg"
            val TYPE_PNG = "image/png"
        }
    }

    data class WaterMarkInfo(
        val waterMarkInfoList: List<Pair<String, String>>?
    ){
        companion object{
            fun EMPTY(): WaterMarkInfo = WaterMarkInfo(emptyList())
        }
    }

}