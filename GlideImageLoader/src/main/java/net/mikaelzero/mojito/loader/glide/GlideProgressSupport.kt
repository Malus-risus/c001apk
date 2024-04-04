package net.mikaelzero.mojito.loader.glide

import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import okhttp3.*
import okio.*
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

object GlideProgressSupport {
    private fun createInterceptor(listener: ResponseProgressListener): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder()
                .body(OkHttpProgressResponseBody(request.url, response.body, listener))
                .build()
        }
    }

    fun init(glide: Glide, okHttpClient: OkHttpClient?) {
        val builder = okHttpClient?.newBuilder() ?: OkHttpClient.Builder()
        builder.addNetworkInterceptor(createInterceptor(DispatchingProgressListener()))
        glide.registry.replace(
            GlideUrl::class.java, InputStream::class.java,
            OkHttpUrlLoader.Factory(builder.build())
        )
    }

    @JvmStatic
    fun forget(url: String) {
        DispatchingProgressListener.forget(url)
    }

    @JvmStatic
    fun expect(url: String, listener: ProgressListener?) {
        DispatchingProgressListener.expect(url, listener)
    }

    interface ProgressListener {
        fun onDownloadStart()
        fun onProgress(progress: Int)
        fun onDownloadFinish()
    }

    private interface ResponseProgressListener {
        fun update(
            url: HttpUrl,
            bytesRead: Long,
            contentLength: Long
        )
    }

    private class DispatchingProgressListener : ResponseProgressListener {
        companion object {
            private val listeners: ConcurrentHashMap<String, ProgressListener?> = ConcurrentHashMap()
            private val progresses: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

            fun forget(url: String) {
                val key = url.substringBefore("?")
                listeners.remove(key)
                progresses.remove(key)
            }

            fun expect(url: String, listener: ProgressListener?) {
                listeners[url.substringBefore("?")] = listener
            }
        }

        override fun update(url: HttpUrl, bytesRead: Long, contentLength: Long) {
            val key = url.toString().substringBefore("?")
            val listener = listeners[key] ?: return
            val lastProgress = progresses[key]
            if (lastProgress == null) {
                // ensure `onStart` is called before `onProgress` and `onFinish`
                listener.onDownloadStart()
            }
            if (contentLength <= bytesRead) {
                listener.onDownloadFinish()
                forget(key)
                return
            }
            val progress = (bytesRead.toFloat() / contentLength * 100).toInt()
            if (lastProgress == null || progress != lastProgress) {
                progresses[key] = progress
                listener.onProgress(progress)
            }
        }
    }

    private class OkHttpProgressResponseBody internal constructor(
        private val url: HttpUrl, private val responseBody: ResponseBody?, 
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
                bufferedSource = source(responseBody?.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source?): Source {
            return object : ForwardingSource(source) {
                private var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    val fullLength = responseBody?.contentLength() ?: -1
                    if (bytesRead == -1L) { 
                        totalBytesRead = fullLength
                    } else {
                        totalBytesRead += bytesRead
                    }
                    progressListener.update(url, totalBytesRead, fullLength)
                    return bytesRead
                }
            }
        }
    }
}
