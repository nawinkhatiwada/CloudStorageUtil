package com.evolveasia.gCloudStorageUtils

import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UploadService {

    @POST("upload/storage/v1/b/{bucketName}/o?uploadType=multipart&predefinedAcl=publicRead")
    fun uploadImageToGCS(
        @Path(value = "bucketName", encoded = true) bucketName: String,
        @Body params: RequestBody
    ): Single<ResponseBody>

    @GET("storage/v1/b/{bucketName}/o")
    fun fetchImage(
        @Path(value = "bucketName", encoded = true) bucketName: String,
        @Query("prefix") query: String
    ): Single<StorageObjectBaseResponse>
}