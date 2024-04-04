package net.mikaelzero.mojito.loader.glide

import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import okhttp3.*
import okio.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object GlideProgressSupport {

    fun init(glide: Glide, okHttpClient: OkHttpClient?) {
        val builder: OkHttpClient.Builder = okHttpClient?.newBuilder() ?: OkHttpClient.Builder()
        builder.addNetworkInterceptor(createInterceptor(DispatchingProgressListener()))
        glide.registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(builder.build()))
    }

    fun forget(url: String) {
        DispatchingProgressListener.forget(url)
    }

    fun expect(url: String, listener: ProgressListener?) {
        DispatchingProgressListener.expect(url, listener)
    }

    private fun createInterceptor(listener: ResponseProgressListener): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder()
                .body(
                    OkHttpProgressResponseBody(
                        request.url, response.body,
                        listener
                    )
                )
                .build()
        }
    }

    interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }

    private class DispatchingProgressListener : ResponseProgressListener {
        companion object {
            private val LISTENERS: ConcurrentHashMap<String, ProgressListener> = ConcurrentHashMap()
            private val PROGRESSES: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

            fun forget(url: String) {
                val key = url.split("\\?")[0]
                LISTENERS.remove(key)
                PROGRESSES.remove(key)
            }

            fun expect(url: String, listener: ProgressListener?) {
                if(listener != null) {
                    LISTENERS[url.split("\\?")[0]] = listener
                }
            }
        }

        override fun update(url: HttpUrl, bytesRead: Long, contentLength: Long) {
            val key = url.toString().split("\\?")[0]
            val listener = LISTENERS[key] ?: return

            var lastProgress = PROGRESSES[key]
            val progress = (100 * bytesRead / contentLength).toInt()

            if (lastProgress == null && bytesRead > 0) {
                // this is a new request so emit the first progress update
                listener.update(bytesRead, contentLength, bytesRead == contentLength)
                lastProgress = progress
                PROGRESSES[key] = lastProgress
            }

            if (progress != lastProgress) {
                listener.update(bytesRead, contentLength, bytesRead == contentLength)
                PROGRESSES[key] = progress
            }

            if (bytesRead == contentLength) {
                forget(key)
            }
        }
    }

    private class OkHttpProgressResponseBody(
        private val url: HttpUrl,
        private val responseBody: ResponseBody?,
        private val progressListener: ResponseProgressListener
    ) : ResponseBody() {

        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody?.contentType()
        }

        override fun contentLength(): Long {
            return responseBody?.contentLength() ?: -1
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = object : ForwardingSource(responseBody!!.source()) {
                    private var totalBytesRead: Long = 0

                    @Throws(IOException::class)
                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val bytesRead = super.read(sink, byteCount)
                        val fullLength = responseBody.contentLength()
                        if (bytesRead != -1L) {
                            totalBytesRead += bytesRead
                            progressListener.update(url, totalBytesRead, fullLength)
                        }
                        return bytesRead
                    }
                }.buffer()
            }
            return bufferedSource!!
        }
    }

    private interface ResponseProgressListener {
        fun update(url: HttpUrl, bytesRead: Long, contentLength: Long)
    }
}
