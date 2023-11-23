package com.milkcocoa.info.milkrop.gesture

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.atan2

/**
 * RotationGestureListener
 * @author keita
 * @since 2023/11/23 01:12
 */

/**
 *
 */
interface RotationGestureListener {
    fun onRotationBegin()
    fun onRotationEnd()
    fun onRotate(angle: Float, pivot: PointF)
}

class RotationGestureDetector constructor(private val listener: RotationGestureListener){
    private var prevPoint1: PointF? = null
    private var prevPoint2: PointF? = null
    private var pointerId1: Int? = null
    private var pointerId2: Int? = null

    fun reset(){
        prevPoint1 = null
        prevPoint2 = null
        pointerId1 = null
        pointerId2 = null
    }

    init {
        reset()
    }

    fun detect(event: MotionEvent) = onTouchEvent(event)

    private fun onTouchEvent(event: MotionEvent): Boolean{
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN ->{
                pointerId1 = event.getPointerId(event.actionIndex)
                pointerId2 = null

                prevPoint1 = PointF(
                    event.getX(event.actionIndex),
                    event.getY(event.actionIndex)
                )
                prevPoint2 = null
            }
            MotionEvent.ACTION_POINTER_DOWN ->{
                if(pointerId1 == null){
                    pointerId1 = event.getPointerId(event.actionIndex)

                    prevPoint1 = PointF(
                        event.getX(event.actionIndex),
                        event.getY(event.actionIndex)
                    )
                    pointerId2?.let {
                        prevPoint2 = PointF(
                            event.getX(event.findPointerIndex(it)),
                            event.getY(event.findPointerIndex(it))
                        )

                        listener.onRotationBegin()
                    }
                }else if(pointerId2 == null){
                    pointerId2 = event.getPointerId(event.actionIndex)

                    prevPoint2 = PointF(
                        event.getX(event.actionIndex),
                        event.getY(event.actionIndex)
                    )
                    pointerId1?.let {
                        prevPoint1 = PointF(
                            event.getX(event.findPointerIndex(it)),
                            event.getY(event.findPointerIndex(it))
                        )
                        listener.onRotationBegin()
                    }
                }else{ }
            }
            MotionEvent.ACTION_MOVE ->{
                pointerId1 ?: return true
                pointerId2 ?: return true
                prevPoint1 ?: return true
                prevPoint2 ?: return true

                val point1 = PointF(
                    event.getX(event.findPointerIndex(pointerId1!!)),
                    event.getY(event.findPointerIndex(pointerId1!!))
                )
                val point2 = PointF(
                    event.getX(event.findPointerIndex(pointerId2!!)),
                    event.getY(event.findPointerIndex(pointerId2!!))
                )

                val angle1 = prevPoint2!!.angle(prevPoint1!!)
                val angle2 = point2.angle(point1)
                var delta = angle2 - angle1
                println("angle1: ${angle1}, angle2: ${angle2}")
                if(delta < -180f){
                    delta += 360.0
                }else if(delta > 180f){
                    delta -= 360.0
                }
                listener.onRotate(
                    angle = delta.toFloat(),
                    pivot = point1.center(point2)
                )

                prevPoint1 = point1
                prevPoint2 = point2
            }
            MotionEvent.ACTION_UP ->{
                pointerId1 = null
                pointerId2 = null

            }
            MotionEvent.ACTION_POINTER_UP ->{
                if(pointerId1 == event.getPointerId(event.actionIndex)){
                    pointerId1 = null
                    if(pointerId2 != null){
                        listener.onRotationEnd()
                    }
                }else if(pointerId2 == event.getPointerId(event.actionIndex)){
                    pointerId2 = null
                    if(pointerId1 != null){
                        listener.onRotationEnd()
                    }
                }
            }
        }

        return true
    }

    fun PointF.angle(from: PointF) =
        Math.toDegrees(
            atan2(
                (y - from.y).toDouble(),
                (x - from.x).toDouble()
            ))
    fun PointF.center(from: PointF) = PointF(
        x.plus(from.x).div(2),
        y.plus(from.y).div(2)
    )
}
