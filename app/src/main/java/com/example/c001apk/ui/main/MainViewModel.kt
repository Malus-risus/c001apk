package com.example.c001apk.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.absoluteValue
import kotlin.math.sign

class NestedScrollableHost1 : FrameLayout {
    private var isChildHasSameDirection = true
    private var touchSlop = 0
    private var initialX = 0f
    private var initialY = 0f

    private val parentViewPager: ViewPager2?
        get() {
            var parent = parent
            while (parent is View && parent !is ViewPager2) {
                parent = parent.parent
            }
            return parent as ViewPager2?
        }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private fun canChildScroll(orientation: Int, delta: Float): Boolean {
        val direction = -delta.sign.toInt()
        var child = parent
        while (child is View && child !is ViewPager2) {
            child = child.parent
        }

        return (orientation == 0 && child.canScrollHorizontally(direction)) ||
                (orientation == 1 && child.canScrollVertically(direction))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initialX
                val dy = event.y - initialY
                val isVpHorizontal = parentViewPager?.orientation == ORIENTATION_HORIZONTAL

                val scaledDx = dx.absoluteValue * if (isVpHorizontal && !isChildHasSameDirection) .5f else 1f
                val scaledDy = dy.absoluteValue * if (!isVpHorizontal && !isChildHasSameDirection) .5f else 1f

                if (scaledDx > touchSlop || scaledDy > touchSlop) {
                    if (isVpHorizontal == (scaledDy > scaledDx)) {
                        parent.requestDisallowInterceptTouchEvent(canChildScroll(if (isVpHorizontal) 0 else 1, if (isVpHorizontal) dx else dy))
                    } else {
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }
}
