package com.milkcocoa.info.milkrop.util

import android.graphics.PointF
import android.graphics.RectF
import android.util.SizeF
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * RectUtils
 * @author keita
 * @since 2023/11/26 21:09
 */

/**
 *
 */
object RectUtils {
    fun getCornersFromRect(r: RectF): FloatArray{
        return floatArrayOf(
            r.left, r.top,
            r.right, r.top,
            r.right, r.bottom,
            r.left, r.bottom
        )
    }

    fun getRectSidesFromCorners(corners: FloatArray): SizeF{
        return SizeF(
            sqrt(
                (corners[0] - corners[2]).pow(2) + (corners[1] - corners[3]).pow(2)
            ),
            sqrt(
                (corners[2] - corners[4]).pow(2) + (corners[3] - corners[5]).pow(2)
            )
        )
    }

    fun getCenterFromRect(r: RectF): FloatArray{
        return floatArrayOf(
            r.centerX(),
            r.centerY()
        )
    }

    fun trapToRect(array: FloatArray): RectF{
        val r = RectF(
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )

        1.until(array.size).step(2).forEach { i ->
            val x = round(array[i - 1] * 10).div(10f)
            val y = round(array[i] * 10).div(10f)

            r.left = if(x < r.left) x else r.left
            r.top = if(y < r.top) y else r.top
            r.right = if(x > r.right) x else r.right
            r.bottom = if(y > r.bottom) y else r.bottom
        }
        r.sort()

        return r
    }
}
