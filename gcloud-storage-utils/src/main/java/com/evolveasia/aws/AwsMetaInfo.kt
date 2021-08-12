package com.evolveasia.aws

import android.graphics.Bitmap
import org.json.JSONObject
import kotlin.properties.Delegates

class AwsMetaInfo(var awsFolderPath: String,
                  val imageMetaInfo: ImageMetaInfo,
                  val serviceConfig: AWSConfig) {

    private constructor(builder: Builder) : this(awsFolderPath = builder.awsFolderPath, imageMetaInfo = builder.imageMetaInfo, serviceConfig = builder.serviceConfig)

    class Builder {
        var imageMetaInfo: ImageMetaInfo by Delegates.notNull()
        var awsFolderPath: String by Delegates.notNull()
        var serviceConfig: AWSConfig by Delegates.notNull()
        fun build() = AwsMetaInfo(this)
    }

    class ImageMetaInfo {
        var imagePath: String by Delegates.notNull()
        var mediaType: String by Delegates.notNull()
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
    ) {
        companion object {
            fun EMPTY(): WaterMarkInfo = WaterMarkInfo(emptyList())
        }
    }

    class AWSConfig(val awsConfiguration: JSONObject,
                    val bucketName: String,
                    val cognitoPoolId: String,
                    var region: String)

}