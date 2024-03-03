package net.mikaelzero.mojito.view.sketch

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.lifecycle.LifecycleObserver
import net.mikaelzero.mojito.Mojito.Companion.mojitoConfig
import net.mikaelzero.mojito.interfaces.OnMojitoViewCallback
import net.mikaelzero.mojito.loader.ContentLoader
import net.mikaelzero.mojito.loader.OnLongTapCallback
import net.mikaelzero.mojito.loader.OnTapCallback
import net.mikaelzero.mojito.tools.ScreenUtils
import net.mikaelzero.mojito.view.sketch.core.SketchImageView
import net.mikaelzero.mojito.view.sketch.core.decode.ImageSizeCalculator
import kotlin.math.abs


/**
 * @Author: MikaelZero
 * @CreateDate: 2020/6/10 10:01 AM
 * @Description:
 */
class SketchContentLoaderImpl : ContentLoader, LifecycleObserver {


    private lateinit var sketchImageView: SketchImageView
    private lateinit var frameLayout: FrameLayout
    private var isLongHeightImage = false
    private var isLongWidthImage = false
    private var screenHeight = 0
    private var screenWidth = 0
    private var longImageHeightOrWidth = 0
    private var onMojitoViewCallback: OnMojitoViewCallback? = null

    override fun providerRealView(): View {
        return sketchImageView
    }

    override fun providerView(): View {
        return frameLayout
    }

    override val displayRect: RectF
        get() {
            val rectF = RectF()
            sketchImageView.zoomer?.getDrawRect(rectF)
            return RectF(rectF)
        }

    override fun init(context: Context, originUrl: String, targetUrl: String?, onMojitoViewCallback: OnMojitoViewCallback?) {
        frameLayout = FrameLayout(context)
        sketchImageView = SketchImageView(context)
        sketchImageView.isZoomEnabled = true
        sketchImageView.options.isDecodeGifImage = true
        frameLayout.addView(sketchImageView)
        screenHeight = if (mojitoConfig().transparentNavigationBar()) ScreenUtils.getScreenHeight(context) else ScreenUtils.getAppScreenHeight(context)
        screenWidth = ScreenUtils.getScreenWidth(context)
        this.onMojitoViewCallback = onMojitoViewCallback
        sketchImageView.zoomer?.blockDisplayer?.setPause(true)
    }

    override fun dispatchTouchEvent(
    isDrag: Boolean,
    isActionUp: Boolean,
    isDown: Boolean,
    isHorizontal: Boolean
): Boolean {
    val zoomer = sketchImageView.zoomer ?: return false
    val rectF = Rect().apply { zoomer.getVisibleRect(this) }
    val drawRect = RectF().apply { zoomer.getDrawRect(this) }
    
    // Common checks for scaling and position
    val isScale = zoomer.maxZoomScale - zoomer.zoomScale > 0.01f
    val isAtFullZoomScale = zoomer.zoomScale == zoomer.fillZoomScale

    // Callback function for long image movement
    fun onLongImageMove(ratio: Float) {
        onMojitoViewCallback?.onLongImageMove(ratio)
    }

    return when {
        isLongHeightImage -> when {
            isDrag -> false
            isActionUp -> !isDrag
            else -> {
                onLongImageMove(abs(drawRect.top) / (longImageHeightOrWidth - screenHeight))

                val isTop = isAtFullZoomScale && rectF.top == 0 && isDown
                val isCenter = isAtFullZoomScale && rectF.top != 0 && screenHeight != drawRect.bottom.toInt()
                val isBottom = isAtFullZoomScale && !isDown && screenHeight == drawRect.bottom.toInt()

                isTop || isCenter || isScale || isBottom
            }
        }
        isLongWidthImage -> {
            if (isDrag && !isHorizontal) return false
            if (isActionUp) return !isDrag

            onLongImageMove(abs(drawRect.left) / abs(drawRect.right - drawRect.left))
            isHorizontal || isScale
        }
        else -> zoomer.zoomScale > zoomer.fullZoomScale && zoomer.zoomScale - zoomer.fullZoomScale > 0.01f
    }

    override fun dragging(width: Int, height: Int, ratio: Float) {
    }

    override fun beginBackToMin(isResetSize: Boolean) {
        sketchImageView.zoomer?.blockDisplayer?.setPause(true)
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            if (isResetSize) {
                sketchImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun backToNormal() {
    }

    override fun loadAnimFinish() {
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            sketchImageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        sketchImageView.zoomer?.blockDisplayer?.setPause(false)
    }

    override fun needReBuildSize(): Boolean {
        return sketchImageView.zoomer!!.zoomScale >= sketchImageView.zoomer!!.fullZoomScale
    }

    override fun useTransitionApi(): Boolean {
        return isLongWidthImage || isLongHeightImage || needReBuildSize()
    }

    override fun isLongImage(width: Int, height: Int): Boolean {
        isLongHeightImage = height > width * 22f / 9f
        isLongWidthImage = width > height * 5 && width > (ScreenUtils.getScreenWidth(sketchImageView.context) * 1.5)
        sketchImageView.zoomer?.isReadMode = isLongHeightImage || isLongWidthImage
        if (isLongWidthImage) {
            longImageHeightOrWidth = width
        } else if (isLongHeightImage) {
            longImageHeightOrWidth = height
        }
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            sketchImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return isLongHeightImage || isLongWidthImage
    }

    override fun onTapCallback(onTapCallback: OnTapCallback) {
        sketchImageView.zoomer?.setOnViewTapListener { view, x, y ->
            onTapCallback.onTap(view, x, y)
        }
    }

    override fun onLongTapCallback(onLongTapCallback: OnLongTapCallback) {
        sketchImageView.zoomer?.setOnViewLongPressListener { view, x, y ->
            onLongTapCallback.onLongTap(view, x, y)
        }
    }

    override fun pageChange(isHidden: Boolean) {

    }

}
