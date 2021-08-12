package com.evolveasia.gCloudStorageUtils

import com.evolveasia.cloudutil_lib.BuildConfig
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ApiModule {

    companion object {

        private fun provideOkHttpClient(serviceConfig: InputStream): OkHttpClient {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            return OkHttpClient.Builder()
                .apply {
                    addInterceptor(ApiInterceptor(serviceConfig))
                    addInterceptor(interceptor)
                    readTimeout(120, TimeUnit.SECONDS)
                    writeTimeout(120, TimeUnit.SECONDS)
                }.build()
        }

        fun provideApiService(serviceConfig: InputStream): UploadService {
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(provideOkHttpClient(serviceConfig))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .build()

            return retrofit.create(UploadService::class.java)
        }
    }
}


