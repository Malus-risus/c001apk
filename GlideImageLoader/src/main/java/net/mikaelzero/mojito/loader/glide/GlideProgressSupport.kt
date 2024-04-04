package net.mikaelzero.mojito.loader.glide

import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader.Factory
import com.bumptech.glide.load.model.GlideUrl
import okhttp3.*
import okio.*
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GlideProgressSupport {
    private val LISTENERS = Collections.synchronizedMap(HashMap<String, ProgressListener>())
    private val PROGRESSES = ConcurrentHashMap<String, Int>()

    fun init(glide: Glide, okHttpClient: OkHttpClient) {
        val client = okHttpClient.newBuilder()
            .addNetworkInterceptor(createInterceptor(DispatchingProgressListener()))
            .build()
        glide.registry.replace(GlideUrl::class.java, InputStream::class.java, Factory(client))
    }

    @JvmStatic
    fun forget(url: String) {
        val key = url.substringBefore('?')
        LISTENERS.remove(key)
        PROGRESSES.remove(key)
    }

    @JvmStatic
    fun expect(url: String, listener: ProgressListener) {
        val key = url.substringBefore('?')
        LISTENERS[key] = listener
        PROGRESSES[key] = 0 
    }

    private fun createInterceptor(listener: ResponseProgressListener): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder()
                .body(OkHttpProgressResponseBody(request.url, response.body, listener))
                .build()
        }
    }

    interface ProgressListener {
        fun onProgress(progress: Int, bytesRead: Long, contentLength: Long, done: Boolean)
    }

    private interface ResponseProgressListener {
        fun update(url: HttpUrl, bytesRead: Long, contentLength: Long)
    }

    private class DispatchingProgressListener : ResponseProgressListener {
        override fun update(url: HttpUrl, bytesRead: Long, contentLength: Long) {
            val key = url.toString().substringBefore('?')
            val listener = LISTENERS[key] ?: return
            val progress = if (contentLength != -1L) (100 * bytesRead / contentLength).toInt() else 0
            val lastProgress = PROGRESSES.getOrDefault(key, 0)

            if (lastProgress != progress) {
                LISTENERS[key]?.onProgress(progress, bytesRead, contentLength, bytesRead == contentLength)
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
                bufferedSource = responseSource(responseBody!!.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun responseSource(source: Source): Source {
            return object : ForwardingSource(source) {
                var totalBytesRead: Long = 0

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0

                    progressListener.update(url, totalBytesRead, responseBody!!.contentLength())

                    return bytesRead
                }
            }
        }
    }
}
