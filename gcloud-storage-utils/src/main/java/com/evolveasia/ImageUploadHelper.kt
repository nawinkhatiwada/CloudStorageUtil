package com.evolveasia

import android.graphics.*
import com.evolveasia.aws.AwsMetaInfo
import com.evolveasia.gCloudStorageUtils.GCSMetaInfo
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


@Throws(IOException::class)
fun streamToByteArray(stream: InputStream): ByteArray {

    val buffer = ByteArray(1024)
    val os = ByteArrayOutputStream()

    var line = 0
    // read bytes from stream, and store them in buffer
    while (line != -1) {
        // Writes bytes from byte array (buffer) into output stream.
        os.write(buffer, 0, line)
        line = stream.read(buffer)
    }
    stream.close()
    os.flush()
    os.close()
    return os.toByteArray()
}


fun decodeSampledBitmapFromResource(data: ByteArray,
                                    reqWidth: Int,
                                    reqHeight: Int,
                                    waterMarkInfo: GCSMetaInfo.WaterMarkInfo?): Bitmap {

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(data, 0, data.size, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    return waterMarkInfo?.let {
        addWaterMark(waterMarkInfo, data, 0, data.size, options)
    } ?: BitmapFactory.decodeByteArray(data, 0, data.size, options)

}

fun decodeSampledBitmapFromResource(data: ByteArray,
                                    reqWidth: Int,
                                    reqHeight: Int,
                                    waterMarkInfo: AwsMetaInfo.WaterMarkInfo?): Bitmap {

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(data, 0, data.size, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    /*  return waterMarkInfo?.let {
          addAwsWaterMark(waterMarkInfo, data, 0, data.size, options)
      } ?: BitmapFactory.decodeByteArray(data, 0, data.size, options)
  */
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight = height / 2
        val halfWidth = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}


fun addWaterMark(
        imageMetaData: GCSMetaInfo.WaterMarkInfo,
        data: ByteArray,
        offset: Int,
        size: Int,
        options: BitmapFactory.Options
): Bitmap {

    val bm = BitmapFactory.decodeByteArray(data, offset, size, options)
    val w = bm.width
    val h = bm.height
    val result = Bitmap.createBitmap(w, h, bm.config)

    val canvas = Canvas(result)
    val blurMaskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)

    val backgroundPaint = Paint()
    backgroundPaint.color = Color.BLACK
    backgroundPaint.alpha = 50

    val paint = Paint()
    paint.color = Color.RED
    paint.textSize = 30f
    paint.isAntiAlias = true

    canvas.drawBitmap(bm, 0f, 0f, paint)
    backgroundPaint.maskFilter = blurMaskFilter
    val fontMetrics = Paint.FontMetrics()
    var yAxisPosition = h - 16
    paint.getFontMetrics(fontMetrics)
    paint.color = Color.WHITE

    imageMetaData.waterMarkInfoList?.asReversed()?.forEach {
        val value = it.second
        canvas.drawRect(0f, yAxisPosition + fontMetrics.top, paint.measureText(value), yAxisPosition + fontMetrics.bottom, backgroundPaint)
        canvas.drawText(
                value,
                16f,
                yAxisPosition.toFloat(),
                paint)
        yAxisPosition -= 35

    }
    return result
}

fun addAwsWaterMark(
        awsMetaInfo: AwsMetaInfo,
        bitmap: Bitmap,
): Bitmap {

    val w = bitmap.width
    val h = bitmap.height
    val result = Bitmap.createBitmap(w, h, bitmap.config)

    val canvas = Canvas(result)
    val blurMaskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)

    val backgroundPaint = Paint()
    backgroundPaint.color = Color.BLACK
    backgroundPaint.alpha = 50

    val paint = Paint()
    paint.color = Color.RED
    paint.textSize = 30f
    paint.isAntiAlias = true

    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    backgroundPaint.maskFilter = blurMaskFilter
    val fontMetrics = Paint.FontMetrics()
    var yAxisPosition = h - 16
    paint.getFontMetrics(fontMetrics)
    paint.color = Color.WHITE

    awsMetaInfo.imageMetaInfo.waterMarkInfo?.waterMarkInfoList?.asReversed()?.forEach {
        val value = it.second
        canvas.drawRect(0f, yAxisPosition + fontMetrics.top, paint.measureText(value), yAxisPosition + fontMetrics.bottom, backgroundPaint)
        canvas.drawText(
                value,
                16f,
                yAxisPosition.toFloat(),
                paint)
        yAxisPosition -= 35

    }
    val out = FileOutputStream(awsMetaInfo.imageMetaInfo.imagePath)
    result.compress(Bitmap.CompressFormat.JPEG, 80, out)
    out.flush()
    out.close()
    bitmap.recycle()
    return result
}
