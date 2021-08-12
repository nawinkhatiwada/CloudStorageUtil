package com.evolveasia.gCloudStorageUtils

import com.google.gson.annotations.SerializedName

data class StorageObjectBaseResponse(
    @SerializedName("kind") var kind: String,
    @SerializedName("items") var storageObjectResponseList: List<StorageObjectResponseModel>
)

data class StorageObjectResponseModel(
    @SerializedName("id") var id: String,
    @SerializedName("name") var name: String,
    @SerializedName("bucket") var bucket: String,
    @SerializedName("contentType") var contentType: String,
    @SerializedName("mediaLink") var url: String,
    @SerializedName("timeCreated") var timeCreated: String
){

    fun getImageTimeStamp(): String {
        val stream = name.split("_")
        val index = stream.lastIndex
        return stream[index].split(".")[0]
    }

    fun getImageCategory(): Int {
        val stream = name.split("_")
        val index = stream.lastIndex - 1
        return stream[index].toInt()
    }

    fun getDirectoryParent() = name.split("/")[0]

    fun isClosed() = name.contains("closed", true) || name.contains("close")

}

infix fun StorageObjectResponseModel.isFromDirectory(query: String) = getDirectoryParent() == query && (contentType == "image/jpeg" || contentType == "image/png")