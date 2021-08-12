/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.evolveasia.aws

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.evolveasia.initializer.ContextProviderImpl.Companion.getInstance
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/*
 * Handles basic helper functions used throughout the app.
 */
object Util {
    // We only need one instance of the clients and credentials provider
    private var sS3Client: AmazonS3Client? = null
    private var sCredProvider: CognitoCachingCredentialsProvider? = null
    private var sTransferUtility: TransferUtility? = null

    /**
     * Gets an instance of CognitoCachingCredentialsProvider which is
     * constructed using the given Context.
     *
     * @param
     * @return A default credential provider.
     */
    private fun getCredProvider(awsMetaInfo: AwsMetaInfo.AWSConfig): CognitoCachingCredentialsProvider? {
        if (sCredProvider == null) {
            sCredProvider = CognitoCachingCredentialsProvider(
                    getInstance().getAppCtx(),
                    awsMetaInfo.cognitoPoolId,
                    Regions.fromName(awsMetaInfo.region))
        }
        return sCredProvider
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @param
     * @return A default S3 client.
     */
    fun getS3Client(awsMetaInfo: AwsMetaInfo.AWSConfig): AmazonS3Client {
        if (sS3Client == null) {
            sS3Client = AmazonS3Client(getCredProvider(awsMetaInfo), Region.getRegion(awsMetaInfo.region))
        }
        return sS3Client!!
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @return a TransferUtility instance
     */
    @JvmStatic
    fun getTransferUtility(awsMetaInfo: AwsMetaInfo): TransferUtility? {
        if (sTransferUtility == null) {
            sTransferUtility = TransferUtility.builder()
                    .context(getInstance().getAppCtx())
                    .s3Client(getS3Client(awsMetaInfo.serviceConfig))
                    .awsConfiguration(AWSConfiguration(awsMetaInfo.serviceConfig.awsConfiguration))
                    .build()
        }
        return sTransferUtility
    }

    /**
     * Converts number of bytes into proper scale.
     *
     * @param bytes number of bytes to be converted.
     * @return A string that represents the bytes in a proper scale.
     */
    private fun getBytesString(bytes: Long): String {
        val quantifiers = arrayOf(
                "KB", "MB", "GB", "TB"
        )
        var speedNum = bytes.toDouble()
        var i = 0
        while (true) {
            if (i >= quantifiers.size) {
                return ""
            }
            speedNum /= 1024.0
            if (speedNum < 512) {
                return String.format("%.2f", speedNum) + " " + quantifiers[i]
            }
            i++
        }
    }

    /*
     * Fills in the map with information in the observer so that it can be used
     * with a SimpleAdapter to populate the UI
     */
    fun fillMap(map: MutableMap<String?, Any?>, observer: TransferObserver, isChecked: Boolean) {
        val progress = (observer.bytesTransferred.toDouble() * 100 / observer
                .bytesTotal).toInt()
        map["id"] = observer.id
        map["checked"] = isChecked
        map["fileName"] = observer.absoluteFilePath
        map["progress"] = progress
        map["bytes"] = (getBytesString(observer.bytesTransferred) + "/"
                + getBytesString(observer.bytesTotal))
        map["state"] = observer.state
        map["percentage"] = "$progress%"
    }

    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                ?: return false
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}