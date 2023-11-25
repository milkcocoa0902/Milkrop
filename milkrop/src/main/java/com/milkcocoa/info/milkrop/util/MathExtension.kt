package com.milkcocoa.info.milkrop.util

import android.graphics.PointF
import android.util.Size
import android.util.SizeF
import com.milkcocoa.info.milkrop.model.DistanceF

/**
 * MathExtention
 * @author keita
 * @since 2023/11/23 22:45
 */

/**
 *
 */
object MathExtension {
    fun<T: Number> T.rangeIn(min: T, max: T, eps: Double): Boolean{
        return min.toDouble().minus(eps) <= this.toDouble() && this.toDouble() <= max.toDouble().plus(eps)
    }
    fun<T: Number> T.rangeOut(min: T, max: T, eps: Double): Boolean = rangeIn(min, max, eps).not()

    internal fun<T: Number> Size.times(factor: T) = Size(this.width.times(factor.toDouble()).toInt(), this.height.times(factor.toDouble()).toInt())
    internal fun<T: Number> SizeF.times(factor: T) = SizeF(this.width.times(factor.toDouble()).toFloat(), this.height.times(factor.toDouble()).toFloat())


    internal fun PointF.plus(other: DistanceF) = PointF(this.x + other.x, this.y + other.y)
    internal fun PointF.minus(other: DistanceF) = PointF(this.x - other.x, this.y - other.y)

    internal fun PointF.minus(other: PointF) = DistanceF(this.x - other.x, this.y - other.y)

}
