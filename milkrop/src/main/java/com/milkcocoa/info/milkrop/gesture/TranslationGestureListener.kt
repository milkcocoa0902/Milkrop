package com.milkcocoa.info.milkrop.gesture

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * TranslationGestureListener
 * @author keita
 * @since 2023/11/23 01:41
 */

/**
 *
 */
interface TranslationGestureListener {
    fun onScroll(distanceX: Float, distanceY: Float)
    fun onDoubleTap(touchPoint: PointF)
}

class TranslationGestureDetector(context: Context, private val listener: TranslationGestureListener){
    fun reset(){

    }

    fun detect(event: MotionEvent) = detector.onTouchEvent(event)
    private val detector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener(){
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if(e1?.getPointerId(0) == e2.getPointerId(0)){
                listener.onScroll(distanceX, distanceY)
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            listener.onDoubleTap(PointF(e.rawX, e.rawY))
            return super.onDoubleTap(e)
        }
    })
}
