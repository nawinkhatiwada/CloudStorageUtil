package com.evolveasia.gCloudStorageUtils

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.InputStream

class ApiInterceptor(val serviceConfig: InputStream): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = "Bearer " + getAuthCredentials(serviceConfig).accessToken.tokenValue
        val multipartRelated = "multipart/related; boundary=__XX__"
        val requestBuilder = chain.request().newBuilder()
        requestBuilder.apply{
            addHeader("Authorization", token)
            addHeader("Content-Type", multipartRelated)
        }

        val response = chain.proceed(requestBuilder.build())
        val responseBody = response.body
        val responseString = responseBody?.string()

        val contentType = responseBody?.contentType()
        return response.newBuilder().body(ResponseBody.create(contentType, responseString!!)).build()


    }
}