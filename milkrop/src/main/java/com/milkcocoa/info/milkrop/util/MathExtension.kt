package com.milkcocoa.info.milkrop.util

/**
 * MathExtention
 * @author keita
 * @since 2023/11/23 22:45
 */

/**
 *
 */
object MathExtension {
    fun<T: Number> Int.rangeIn(min: T, max: T): Boolean{
        return min.toDouble() <= this && this <= max.toDouble()
    }
    fun<T: Number> Int.rangeOut(min: T, max: T): Boolean = rangeIn(min, max).not()


    fun<T: Number> Float.rangeIn(min: T, max: T): Boolean{
        return min.toDouble() <= this.toDouble() && this <= max.toDouble()
    }
    fun<T: Number> Float.rangeOut(min: T, max: T): Boolean = rangeIn(min, max).not()

    fun<T: Number> Double.rangeIn(min: T, max: T): Boolean{
        return min.toDouble() <= this && this <= max.toDouble()
    }

    fun<T: Number> Double.rangeOut(min: T, max: T): Boolean = rangeIn(min, max).not()
}
