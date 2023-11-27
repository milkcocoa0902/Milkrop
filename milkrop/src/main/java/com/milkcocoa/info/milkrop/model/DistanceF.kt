package com.milkcocoa.info.milkrop.model

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * DistanceF
 * @author keita
 * @since 2023/11/25 18:27
 */

/**
 *
 */
data class DistanceF(val x: Float, val y: Float){
    fun plus(other: DistanceF) = DistanceF(x.plus(other.x), y.plus(other.y))
    fun<T: Number> times(factor: T) = DistanceF(x.times(factor.toDouble()).toFloat(), y.times(factor.toDouble()).toFloat())
    fun length() = sqrt(x.toDouble().pow(2) + y.toDouble().pow(2))
}

