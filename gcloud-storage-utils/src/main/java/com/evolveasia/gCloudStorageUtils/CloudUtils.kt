package com.evolveasia.gCloudStorageUtils

import com.evolveasia.aws.AwsMetaInfo
import com.evolveasia.aws.TransferObservable
import com.evolveasia.aws.TransferUpdate
import com.evolveasia.decodeSampledBitmapFromResource
import com.evolveasia.gCloudStorageUtils.Constants.DEFAULT_IMAGE_HEIGHT
import com.evolveasia.gCloudStorageUtils.Constants.DEFAULT_IMAGE_WIDTH
import com.evolveasia.gCloudStorageUtils.Constants.TYPE_JSON
import com.evolveasia.streamToByteArray
import com.google.api.services.storage.StorageScopes
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.OAuth2Credentials
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.*
import java.util.*

/**
 * @param [serviceConfig] : [InputStream] of the service key json
 * @return [OAuth2Credentials] for the storage app
 */
fun getAuthCredentials(serviceConfig: InputStream?): OAuth2Credentials {
    return GoogleCredentials.fromStream(serviceConfig)
            .createScoped(Collections.singleton(StorageScopes.DEVSTORAGE_FULL_CONTROL))
            .apply { refresh() }
}


fun getMetaDataRequestBody(gcsMetaInfo: GCSMetaInfo, emitter: FlowableEmitter<Double>): RequestBody {
    val jsonParams = JSONObject().apply {
        put("name", gcsMetaInfo.gcsStoragePath)
        if (gcsMetaInfo.imageMetaInfo.metadata.isEmpty()) return@apply
        put("metadata", gcsMetaInfo.imageMetaInfo.metadata)
    }
    return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(RequestBody.create(TYPE_JSON, jsonParams.toString()))
            .addPart(RequestBody.create(gcsMetaInfo.imageMetaInfo.mediaType.toMediaTypeOrNull(), gcsMetaInfo.imageMetaInfo.imageByteArray))
            .build()
}

//this is for with progress
//createCountingRequestBody(File(authInfo.content.name),emitter)
//this is for with out progress
//RequestBody.create(TYPE_CONTENT, authInfo.content.imageByteArray)


private fun createCountingRequestBody(file: File, emitter: FlowableEmitter<Double>): RequestBody {
    val requestBody = createRequestBody(file)
    println("create couting req body")
    print("image file name --> ${file.absolutePath}")
    return CountingRequestBody(requestBody) { bytesWritten, contentLength ->
        val progress: Double = (1.0 * bytesWritten) / contentLength
        println("prog: $progress")
        emitter.onNext(progress)
    }
}


private fun createRequestBody(file: File): RequestBody {
    return RequestBody.create(GCSMetaInfo.ImageMetaInfo.TYPE_JPEG.toMediaTypeOrNull(), file)
}


fun uploadImageGCS(gcsMetaInfo: GCSMetaInfo): Flowable<Double> {
    return Flowable.create({ emitter ->
        gcsMetaInfo.imageMetaInfo.imageByteArray = compressImage(gcsMetaInfo)
        val params = getMetaDataRequestBody(gcsMetaInfo, emitter)
        ApiModule.provideApiService(gcsMetaInfo.serviceConfig).uploadImageToGCS(gcsMetaInfo.bucketName, params)
                .subscribe({
                    emitter.onComplete()
                }, {
                    emitter.onError(it)
                })
        // TODO: find solution for tryOnError i.e. emitter::tryOnError not working

    }, BackpressureStrategy.LATEST)
}

fun compressImage(gcsMetaInfo: GCSMetaInfo): ByteArray {
    val byteArray = streamToByteArray(FileInputStream(gcsMetaInfo.imageMetaInfo.imagePath))
    val bitmap = decodeSampledBitmapFromResource(byteArray, gcsMetaInfo.imageMetaInfo.imageWidth
            ?: DEFAULT_IMAGE_WIDTH, gcsMetaInfo.imageMetaInfo.imageHeight
            ?: DEFAULT_IMAGE_HEIGHT, gcsMetaInfo.imageMetaInfo.waterMarkInfo)

    val stream = ByteArrayOutputStream()
    bitmap.compress(gcsMetaInfo.imageMetaInfo.compressFormat, gcsMetaInfo.imageMetaInfo.compressLevel, stream)

    return stream.toByteArray()
}



fun fetchImage(bucketName: String, serviceConfig: InputStream, query: String): Single<StorageObjectBaseResponse> {
    return ApiModule.provideApiService(serviceConfig)
            .fetchImage(bucketName, query)

}



