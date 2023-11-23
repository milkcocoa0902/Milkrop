package com.milkcocoa.info.milkrop.gesture

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener

/**
 * ScaleGestureListener
 * @author keita
 * @since 2023/11/23 01:16
 */

/**
 *
 */
interface ScaleGestureListener {
    fun onScaleBegin()
    fun onScale(scaleFactor: Float, touchPoint: PointF)
    fun onScaleEnd()
}

class ScaleGestureDetector(private val context: Context, listener: ScaleGestureListener){
    private var touchPoint = PointF(0.0f, 0.0f)
    private var scaleFactor = 1.0f

    fun reset(){
        scaleFactor = 1.0f
        touchPoint = PointF(0.0f, 0.0f)
    }

    fun detect(event: MotionEvent) =
        detector.onTouchEvent(event)

    private val detector = ScaleGestureDetector(context, object: SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = detector.scaleFactor
            listener.onScale(
                scaleFactor = scaleFactor,
                touchPoint = touchPoint
            )
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            touchPoint = PointF(
                detector.focusX,
                detector.focusY
            )
            listener.onScaleBegin()
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            listener.onScaleEnd()
            super.onScaleEnd(detector)
        }
    })
}
