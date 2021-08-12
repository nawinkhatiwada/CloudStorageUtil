package com.evolveasia.cloudutils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.evolve.rosiautils.PictureManager2
import com.evolveasia.aws.AwsMetaInfo
import com.evolveasia.aws.fetchAwsImage
import com.evolveasia.aws.uploadImageAWS
import com.evolveasia.cloudutils.utils.RmapGcsConstants
import com.evolveasia.gCloudStorageUtils.GCSMetaInfo
import com.evolveasia.gCloudStorageUtils.fetchImage
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_upload.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class UploadActivity : AppCompatActivity() {

    private lateinit var pictureManager: PictureManager2

//    var path: String = ""
    var imagePathList = mutableListOf<String>()

    companion object {
        const val PICK_FROM_CAMERA = 100

        /*
     * You should replace these values with your own. See the README for details
     * on what to fill in.
     */
        const val COGNITO_POOL_ID = "your cognito pool id here"

        /*
     * Note, you must first create a bucket using the S3 console before running
     * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
     * put it's name in the field below.
     */
        const val BUCKET_NAME = "Your bucket name here"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        pictureManager = PictureManager2(this@UploadActivity)
        btn_capture.setOnClickListener {
            onTakePhoto()
        }

        btn_upload.setOnClickListener {
            uploadImages(imagePathList)
        }

        btn_fetch.setOnClickListener {
            fetchAwImage()
        }
    }

    override fun onResume() {
        super.onResume()
        pictureManager.setListener(getImagePathListener())
    }

    private fun onTakePhoto() {
        if (ContextCompat.checkSelfPermission(
                        this@UploadActivity,
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PICK_FROM_CAMERA
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        pictureManager.dispatchTakePictureIntent(imagePathListener = getImagePathListener(), openFrontCamera = false)
    }

    private fun getImagePathListener(): (String) -> Unit {
        return { currentImagePath ->
            if (currentImagePath.isNotEmpty()) {
                if (!imagePathList.contains(currentImagePath)) {
                    imagePathList.add(currentImagePath)
                }
                loadImage(findViewById(R.id.img_preview), currentImagePath)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pictureManager.onActivityResult(requestCode, resultCode, data)
    }
    private fun uploadImages(imagePathList: MutableList<String>){
        println("image path List $imagePathList")

        if(imagePathList.isEmpty()){
            println("image is empty")
            progressBar.visibility = View.GONE

            return
        }
        progressBar.visibility = View.VISIBLE

        val imagePath = imagePathList.first()

        val awsConfig = AwsMetaInfo.AWSConfig(getConfigurationFile(resources.openRawResource(R.raw.awsconfiguration)), BUCKET_NAME, COGNITO_POOL_ID, "your region here")
        val gcsMetaData = AwsMetaInfo.Builder().apply {
            serviceConfig = awsConfig
            this.awsFolderPath = "${getStoragePath()}/${File(imagePath).name}"
            imageMetaInfo = AwsMetaInfo.ImageMetaInfo().apply {
                this.imagePath = imagePath
                this.mediaType = GCSMetaInfo.ImageMetaInfo.TYPE_JPEG
//                this.metadata = imageExtraParams.toString()
                compressLevel = 80
                compressFormat = Bitmap.CompressFormat.JPEG
                waterMarkInfo = getWaterMarkInfo()
            }
        }.build()
         val disposable = uploadImageAWS(gcsMetaData).doOnNext {
             println("on next first")


         }.subscribeOn(Schedulers.computation())
             .observeOn(AndroidSchedulers.mainThread())
             .doOnNext {
                 print("next on call  after main thread $it")
             }
             .subscribe({ it ->
                 println("Progress: $it")

             }, {
                 println("Image error: ${it.localizedMessage}")
                 println("image error stacktrace: ${it.printStackTrace()}:")
                 progressBar.visibility = View.GONE
             }, {
                 println("completed")
                 imagePathList.removeFirst()
                 uploadImages(imagePathList)
//                 progressBar.visibility = View.GONE
             })

    }

    private fun getStoragePath(): String {
        return "test/outlet_profile"
    }

    private fun fetchImage() {
        val serviceConfig = assets.open(RmapGcsConstants.SERVER_KEY)
        var dis = fetchImage(RmapGcsConstants.BUCKET_NAME, serviceConfig, "76569")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    println("complete ${it}")
                }, {
                    it.printStackTrace()
                })
    }

    private fun fetchAwImage() {
        val config = AwsMetaInfo.AWSConfig(getConfigurationFile(resources.openRawResource(R.raw.awsconfiguration)), BUCKET_NAME, COGNITO_POOL_ID, "Your region here")
        var dis = fetchAwsImage(config, "test/outlet_profile/")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    println("complete ${it}")
                }, {
                    it.printStackTrace()
                })
    }

    private fun getWaterMarkInfo(): AwsMetaInfo.WaterMarkInfo {
        val dataList = mutableListOf<Pair<String, String>>()
        dataList.add(Pair("Outlet name", "Test-krishna Store"))
        dataList.add(Pair("Location", "27.345, 85.635"))
        dataList.add(Pair("Time", "2019-12-12 13:24:54"))
        dataList.add(Pair("Verified by", "Test User"))
        return AwsMetaInfo.WaterMarkInfo(dataList)
    }

    private fun getConfigurationFile(inputStream: InputStream): JSONObject {
        val outputStream = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        var len: Int = 0
        try {
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
            }
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return JSONObject(outputStream.toString())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pictureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadImage(imageView: ImageView, path: String?) {
        if (path.isNullOrEmpty())
            return
        if (URLUtil.isHttpsUrl(path) || URLUtil.isHttpUrl(path)) {
            Glide.with(imageView.context).load(path).into(imageView)
            return
        }
      /*  Glide.with(imageView.context)
                .load(path).let { request ->
                    val rotation = getRotation(Uri.parse(path))
                    rotation?.let {
                        request.transform(rotation)
                    }.orElse {
                        request
                    }
                }.into(imageView)*/
        Glide.with(imageView.context)
                .asBitmap().load(path).into(imageView)
    }

    private fun getOrientation(uri: Uri): Int {
        val ei = ExifInterface(uri.path!!)
         val o = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
        )
        println("Orientation ---> $o")
        return o
    }

    private fun getRotation(uri: Uri): Rotate? {
        return when (getOrientation(uri)) {
            ExifInterface.ORIENTATION_ROTATE_270 -> Rotate(270)
            ExifInterface.ORIENTATION_ROTATE_180 -> Rotate(270)
            ExifInterface.ORIENTATION_ROTATE_90 -> null
            ExifInterface.ORIENTATION_NORMAL -> null
            else -> null
        }
    }
}
