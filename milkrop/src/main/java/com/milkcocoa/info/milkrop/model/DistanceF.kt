package com.milkcocoa.info.milkrop.model

/**
 * DistanceF
 * @author keita
 * @since 2023/11/25 18:27
 */

/**
 *
 */
data class DistanceF(val x: Float, val y: Float)

fun<T: Number> DistanceF.times(factor: T) = DistanceF(x.times(factor.toDouble()).toFloat(), y.times(factor.toDouble()).toFloat())
fun DistanceF.plus(other: DistanceF) = DistanceF(x.plus(other.x), y.plus(other.y))
