package com.evolveasia.aws

import android.R.attr.*
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectListing
import com.evolveasia.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

fun uploadImageAWS(awsMetaInfo: AwsMetaInfo): Flowable<String> {
    return Flowable.create({ emitter ->
      /*  val oldExif = ExifInterface(awsMetaInfo.imageMetaInfo.imagePath)
        val compressedImagePath = compressAwsImage(awsMetaInfo).first*/
        val compressedBitmap = compressAwsImage(awsMetaInfo).second
           /* val newExifOrientation = setImageOrientation(oldExif, compressedImagePath)
            if (newExifOrientation != null) {
                try {
                    val rotation = getRotation(newExifOrientation)
                    if (rotation != null) {
                        val matrix = Matrix()
                        matrix.postRotate(rotation)
                        setPostScale(newExifOrientation, matrix)
                        if (compressedBitmap != null) {
                            val rotatedBitmap = Bitmap.createBitmap(compressedBitmap, 0, 0, compressedBitmap.width, compressedBitmap.height, matrix, true)
                            if (rotatedBitmap != null) {
                                // rotatedBitmap will be recycled inside addAwsWaterMark function
                                val waterMarkBitmap = addAwsWaterMark(awsMetaInfo, rotatedBitmap)
                                waterMarkBitmap.recycle()
                            }
                            compressedBitmap.recycle()
                        }
                    } else {*/
                        if (compressedBitmap != null) {
                            val newBitmap = Bitmap.createBitmap(compressedBitmap, 0, 0, compressedBitmap.width, compressedBitmap.height)
                            if (newBitmap != null) {
                                // newBitmap will be recycled inside addAwsWaterMark function
                                val waterMarkBitmap = addAwsWaterMark(awsMetaInfo, newBitmap)
                                waterMarkBitmap.recycle()
                            }
                            compressedBitmap.recycle()
                        }
//                    }
              /*  } catch (error: Exception) {
                    error.printStackTrace()
                    awsMetaInfo.imageMetaInfo.imagePath = compressedImagePath
                }*/
//            }
        amazonUploadSingle(awsMetaInfo)
                ?.subscribeOn(io.reactivex.schedulers.Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    emitter.onComplete()
                }) {
                    it.printStackTrace()
                    emitter.onError(it)
                }

    }, BackpressureStrategy.LATEST)
}

//Warning amazon notifies on main thread
fun amazonUploadSingle(awsMetaInfo: AwsMetaInfo, uploadPath: String, maxFileSize: Long): Single<TransferUpdate?>? {
    return TransferObservable(awsMetaInfo, awsMetaInfo.imageMetaInfo.imagePath, uploadPath, maxFileSize)
            .doOnNext { transferUpdate ->
                if (transferUpdate.state == TransferUpdate.PROGRESSED_CHANGED)
                    println("Data upload Progress Progress:%d , Total:%d" + transferUpdate.byteCount + transferUpdate.byteTotal)
            }.lastOrError().observeOn(io.reactivex.schedulers.Schedulers.io())
}

fun amazonUploadSingle(awsMetaInfo: AwsMetaInfo): Single<TransferUpdate?>? {
    return amazonUploadSingle(awsMetaInfo, awsMetaInfo.awsFolderPath, TransferObservable.FILE_SIZE_UNLIMITED)
}

fun compressAwsImage(awsMetaInfo: AwsMetaInfo): Pair<String, Bitmap?> {

    return try {
        val byteArray = streamToByteArray(FileInputStream(awsMetaInfo.imageMetaInfo.imagePath))
        val bitmap = decodeSampledBitmapFromResource(byteArray, awsMetaInfo.imageMetaInfo.imageWidth
                ?: Constants.DEFAULT_IMAGE_WIDTH, awsMetaInfo.imageMetaInfo.imageHeight
                ?: Constants.DEFAULT_IMAGE_HEIGHT, awsMetaInfo.imageMetaInfo.waterMarkInfo)

        val stream = ByteArrayOutputStream()
        bitmap.compress(awsMetaInfo.imageMetaInfo.compressFormat, awsMetaInfo.imageMetaInfo.compressLevel, stream)
        val os: OutputStream = FileOutputStream(awsMetaInfo.imageMetaInfo.imagePath)
        os.write(stream.toByteArray())
        os.close()
        Pair(awsMetaInfo.imageMetaInfo.imagePath, bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        return Pair(awsMetaInfo.imageMetaInfo.imagePath, null)
    }
}

fun fetchAwsImage(serviceConfig: AwsMetaInfo.AWSConfig, queryPath: String): Single<List<AWSStorageResponse>> {
    return Single.fromCallable {
        return@fromCallable getImageFromS3Buckets(serviceConfig, queryPath)
    }

}


fun getImageFromS3Buckets(serviceConfig: AwsMetaInfo.AWSConfig, queryPath: String): List<AWSStorageResponse> {
    // Queries files in the bucket from S3.
    val s3 = Util.getS3Client(serviceConfig)
    var objectList = s3.listObjects(serviceConfig.bucketName, queryPath)
    val list = objectList.objectSummaries.map {
        AWSStorageResponse(s3.getUrl(serviceConfig.bucketName, it.key).toString(), it.lastModified, it.owner.displayName, it.size)
    }.toMutableList()

    while (!objectList.isTruncated) {
        objectList = getObjectList(objectList, s3)
        list.addAll(objectList.objectSummaries.map {
            AWSStorageResponse(s3.getUrl(serviceConfig.bucketName, it.key).toString(), it.lastModified, it.owner.displayName, it.size)
        })
    }
    return list
}

fun getObjectList(objectListing: ObjectListing, s3: AmazonS3Client): ObjectListing {
    return s3.listNextBatchOfObjects(objectListing)
}

private fun getImageOrientation(exif: ExifInterface): Int {
    return exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL)
}

private fun setImageOrientation(oldExif: ExifInterface, newImagePath: String): Int? {
    val exifOrientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION)
    if (exifOrientation != null) {
        val newExif = ExifInterface(newImagePath)
        newExif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation)
        newExif.saveAttributes()
        return newExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    }
    return null
}

fun getRotation(exifOrientation: Int): Float? {
    return when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        else -> null
    }
}

private fun setPostScale(exifOrientation: Int, matrix: Matrix) {
    when (exifOrientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_TRANSVERSE -> matrix.postScale(-1f, 1f)
    }
}