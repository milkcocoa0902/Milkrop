package com.milkcocoa.info.milkrop.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Size
import com.milkcocoa.info.milkrop.gesture.RotationGestureListener
import com.milkcocoa.info.milkrop.view.GestureImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * BitmapLoader
 * @author keita
 * @since 2023/11/26 13:15
 */

/**
 *
 */
object BitmapLoader {
    interface BitmapLoaderListener{
        fun onError(e: Throwable)
        suspend fun onLoad(bitmap: Bitmap, sampleCount: Int)
    }


    class Option{
        var downSample: Boolean = false

        var minSize: Size = Size(0, 0)
        var maxSize: Size = Size(0, 0)

        var maxScale: Float = Float.POSITIVE_INFINITY
        var minScale: Float = 0f
    }

    fun load(context: Context, uri: Uri, listener: BitmapLoaderListener){
        load(context, uri, defaultOption, listener)
    }

    fun load(context: Context, uri: Uri, listener: BitmapLoaderListener, options: Option.()->Unit){
        load(context, uri, Option().apply(options), listener)
    }

    fun load(context: Context, uri: Uri, options: Option, listener: BitmapLoaderListener){
        val coroutineContext = Dispatchers.IO + Job()
        CoroutineScope(coroutineContext).launch {
            kotlin.runCatching {
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri)?.buffered(8192),
                        null,
                        this
                    )

                    inJustDecodeBounds = false
                    inSampleSize = 1

                    if(options.downSample){
                        if(outHeight > options.maxSize.height || outWidth > options.maxSize.width){
                            val halfHeight: Int = outHeight / 2
                            val halfWidth: Int = outWidth / 2

                            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                            // height and width larger than the requested height and width.
                            while (outHeight / inSampleSize >= options.maxSize.height && outWidth / inSampleSize >= options.maxSize.width) {
                                inSampleSize *= 2
                            }
                        }
                    }


                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri)?.buffered(8192),
                        null,
                        this
                    )?.apply {
                        withContext(Dispatchers.Main){
                            listener.onLoad(this@apply, inSampleSize)
                        }
                    } ?: kotlin.run {
                        listener.onError(IOException())
                    }
                }
            }.getOrElse { listener.onError(it) }
        }
    }

    private val defaultOption = Option().apply {
        downSample = false

        minSize = Size(0, 0)
        maxSize = Size(1920, 1080)

        minScale = 0f
        maxScale = Float.POSITIVE_INFINITY
    }
}
