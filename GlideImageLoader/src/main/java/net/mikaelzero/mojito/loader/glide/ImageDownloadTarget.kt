package net.mikaelzero.mojito.loader.glide

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.util.Util
import net.mikaelzero.mojito.loader.glide.GlideProgressSupport.expect
import net.mikaelzero.mojito.loader.glide.GlideProgressSupport.forget
import java.io.File

abstract class ImageDownloadTarget(
    private val width: Int = Target.SIZE_ORIGINAL,
    private val height: Int = Target.SIZE_ORIGINAL,
    private val mUrl: String
) : Target<File>, GlideProgressSupport.ProgressListener {

    private var request: Request? = null

    init {
        require(Util.isValidDimensions(width, height)) {
            "Width and height must both be > 0 or Target#SIZE_ORIGINAL, but given " +
                    "width: $width and height: $height, either provide dimensions in the constructor " +
                    "or call override()"
        }
    }

    override fun onResourceReady(resource: File, transition: Transition<in File>?) = forget(mUrl)

    override fun onLoadCleared(placeholder: Drawable?) = forget(mUrl)

    override fun onLoadStarted(placeholder: Drawable?) = expect(mUrl, this)

    override fun onLoadFailed(errorDrawable: Drawable?) = forget(mUrl)

    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(width, height)
    }

    override fun removeCallback(cb: SizeReadyCallback) {
        // Do nothing, we never retain a reference to the callback.
    }

    override fun setRequest(request: Request?) {
        this.request = request
    }

    override fun getRequest(): Request? = request

    override fun onStart() {
        // Do nothing.
    }

    override fun onStop() {
        // Do nothing.
    }

    override fun onDestroy() {
        // Do nothing.
    }
}
