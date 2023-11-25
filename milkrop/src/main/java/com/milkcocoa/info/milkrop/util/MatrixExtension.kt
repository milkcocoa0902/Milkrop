package com.milkcocoa.info.milkrop.util

import android.graphics.Matrix
import androidx.annotation.IntRange

/**
 * MatrixExtension
 * @author keita
 * @since 2023/11/25 21:23
 */

/**
 *
 */
object MatrixExtension {
    fun Matrix.get(
        @IntRange(from = 0, to = 8)
        index: Int
    ): Float{
        val v = FloatArray(9)
        this.getValues(v)
        return v[index]
    }
}
