package com.evolveasia.gCloudStorageUtils

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.*
import java.io.IOException

class CountingRequestBody(
        var delegate: RequestBody,
        var listener: (Long, Long) -> Unit
) : RequestBody() {


    override fun contentType(): MediaType? = "image/jpeg".toMediaTypeOrNull()

    override fun contentLength(): Long {
        try {
            return delegate.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }


     inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {

        private var bytesWritten: Long = 0

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            println("byteswritter:$bytesWritten")
            listener(bytesWritten, contentLength())
        }
    }

}