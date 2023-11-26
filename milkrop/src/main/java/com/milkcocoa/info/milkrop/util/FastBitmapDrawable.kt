/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.milkcocoa.info.milkrop.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * FastBitmapDrawable
 * @author keita
 * @since 2023/11/26 13:46
 */

/**
 *
 */
class FastBitmapDrawable: Drawable {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var _bitmap: Bitmap? = null
    private var _alpha = 0
    private var _width = 0
    private var _height = 0

    constructor(bitmap: Bitmap?): super(){
        alpha = 255
        setBitmap(bitmap)
    }

    override fun draw(canvas: Canvas) {
        _bitmap?.let { bitmap ->
            if(bitmap.isRecycled.not()){
                canvas.drawBitmap(bitmap, null, bounds, paint)
            }
        }
    }

    override fun setColorFilter(filter: ColorFilter?) {
        paint.colorFilter = filter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setFilterBitmap(filter: Boolean) {
        paint.isFilterBitmap = filter
    }

    override fun getAlpha(): Int {
        return _alpha
    }

    override fun setAlpha(alpha: Int) {
        _alpha = alpha
        paint.alpha = alpha
    }


    override fun getIntrinsicWidth(): Int {
        return _width
    }
    override fun getIntrinsicHeight(): Int {
        return _height
    }

    override fun getMinimumWidth(): Int {
        return _width
    }

    override fun getMinimumHeight(): Int {
        return _height
    }

    fun getBitmap() = _bitmap

    fun setBitmap(bmp: Bitmap?){
        _bitmap = bmp
        bmp?.let {
            _width = bmp.width
            _height = bmp.height
        } ?: kotlin.run {
            _width = 0
            _height = 0
        }
    }
}
