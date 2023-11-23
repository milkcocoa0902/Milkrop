package com.milkcocoa.info.milkrop.view

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.FloatRange
import com.milkcocoa.info.milkrop.gesture.RotationGestureDetector
import com.milkcocoa.info.milkrop.gesture.RotationGestureListener
import com.milkcocoa.info.milkrop.gesture.ScaleGestureListener
import com.milkcocoa.info.milkrop.gesture.TranslationGestureDetector
import com.milkcocoa.info.milkrop.gesture.TranslationGestureListener
import com.milkcocoa.info.milkrop.util.MathExtension.rangeOut
import java.io.BufferedInputStream
import kotlin.math.min

interface ImageListener{
    fun onError(e: Throwable)
    fun onLoad(bitmap: Bitmap)
}

class GestureImageView: androidx.appcompat.widget.AppCompatImageView, ImageListener {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr )

    enum class LoadingState{
        NO_SOURCE,
        LOADING,
        COMPLETE,
        ERROR,
    }
    private var state = LoadingState.NO_SOURCE


    private var size: Size = Size(0, 0)
    public fun getBitmapSize() = size


    private var imageUri: Uri? = null



    private val bmpMatrix = Matrix()
    private var currentRotation: Float = 0.0f
    private var currentScaleFactor: Float = 1.0f


    private var _maxScale: Float = 3.5f
    fun setMaxScale(
        @FloatRange(from = 0.0)
        value: Float
    ){
        _maxScale = value
        if(currentScaleFactor < _maxScale){
            zoom(maxScale, animation = false)
        }
    }
    public val maxScale get() = _maxScale


    private var _minScale: Float = 0.5f
    fun setMinScale(
        @FloatRange(from = 0.0)
        value: Float
    ){
        _maxScale = value
        if(currentScaleFactor > _minScale){
            zoom(_minScale, animation = false)
        }
    }
    public val minScale get() = _minScale






    override fun getRotation(): Float {
        return currentRotation
    }

    override fun setRotation(rotation: Float){
        currentRotation = rotation
        invalidate()
    }


    private val scaleDetector by lazy {
        com.milkcocoa.info.milkrop.gesture.ScaleGestureDetector(
            context = context,
            listener = object: ScaleGestureListener{
                override fun onScale(scaleFactor: Float, touchPoint: PointF) {
                    zoom(
                        scaleFactor,
                        touchPoint,
                        animation = false
                    )
                }

                override fun onScaleBegin() {
                }

                override fun onScaleEnd() {
                }
            }
        )
    }

    private val translationDetector by lazy {
        TranslationGestureDetector(
            context = context,
            listener = object : TranslationGestureListener{
                override fun onScroll(distanceX: Float, distanceY: Float) {
                    bmpMatrix.postTranslate(
                        -distanceX,
                        -distanceY
                    )
                    invalidate()
                }

                override fun onDoubleTap(touchPoint: PointF) {
                    zoom(
                        scaleFactor = 1.4f,
                        pivot = touchPoint,
                        animation = true
                    )
                }
            }
        )
    }


    fun rotate(
        rotation: Float,
    ){
        rotate(
            rotation,
            PointF(
                x.plus(width.toFloat().div(2)),
                y.plus(height.toFloat().div(2))
            )
        )
    }

    fun rotate(
        rotation: Float,
        pivot: PointF
    ){
        currentRotation += rotation
        bmpMatrix.postRotate(
            rotation,
            pivot.x,
            pivot.y
        )
        invalidate()
    }




    /**
     * @param scaleFactor[Float] scale to multiply
     * @param animation[Boolean] do animation. if false, scaling immediately, false scaling with animation. Note that if use animation, last scale IS NOT match specified, but similar.
     */
    fun zoom(
        @FloatRange(from = 0.0)
        scaleFactor: Float,
        animation: Boolean
    ){
        zoom(
            scaleFactor,
            PointF(
                x.plus(width.toFloat().div(2)),
                y.plus(height.toFloat().div(2))
            ),
            animation
        )
    }


    /**
     * @param scaleFactor[Float] scale to multiply
     * @param pivot[PointF] view's position to scale center.
     * @param animation[Boolean] do animation. if false, scaling immediately, false scaling with animation. Note that if use animation, last scale IS NOT match specified, but similar.
     */
    fun zoom(
        @FloatRange(from = 0.0)
        scaleFactor: Float,
        pivot: PointF,
        animation: Boolean
    ){
        if(currentScaleFactor.rangeOut(minScale, maxScale)){
            return
        }

        if(animation){
            val interpolator = AccelerateDecelerateInterpolator()
            val startTime = System.currentTimeMillis()
            val duration = 250L


            val targetScaleFactor = (currentScaleFactor * scaleFactor).coerceIn(minScale, maxScale)
            val initialScaleFactor = currentScaleFactor
            var lastScaleFactor = initialScaleFactor

            currentScaleFactor = targetScaleFactor

            var realScaleFactor = 1.0f

            post(object: Runnable{
                override fun run() {
                    val t = (System.currentTimeMillis() - startTime).toFloat().div(duration).coerceAtMost(1.0f)

                    val interpolatorRatio = interpolator.getInterpolation(t)
                    val tmpScaleFactor = (initialScaleFactor + interpolatorRatio.times(targetScaleFactor - initialScaleFactor)).let {
                        it.div(lastScaleFactor).apply {
                            lastScaleFactor = it
                        }
                    }

                    realScaleFactor *= tmpScaleFactor


                    bmpMatrix.postScale(tmpScaleFactor, tmpScaleFactor, pivot.x, pivot.y)
                    invalidate()
                    if(t < 1.0f){
                        post(this)
                    }else{
                        initialScaleFactor.times(realScaleFactor).coerceIn(minScale, maxScale).div(targetScaleFactor).let { errorFactor ->
                            bmpMatrix.postScale(
                                errorFactor,
                                errorFactor,
                                pivot.x,
                                pivot.y
                            )
                            invalidate()
                        }
                    }
                }
            })
        }else{
            val limitedScaleFactor =
                currentScaleFactor.times(scaleFactor)
                    .coerceIn(minScale, maxScale)
                    .div(currentScaleFactor)

            // safe from exceed limit, like 3.5000002(calc error)
            currentScaleFactor = currentScaleFactor.times(limitedScaleFactor).coerceIn(minScale, maxScale)

            bmpMatrix.postScale(
                limitedScaleFactor,
                limitedScaleFactor,
                pivot.x,
                pivot.y
            )
            invalidate()
        }
    }

    private val rotationDetector by lazy {
        RotationGestureDetector(
            listener = object : RotationGestureListener{
                override fun onRotationBegin() {
                }

                override fun onRotationEnd() {
                }

                override fun onRotate(angle: Float, pivot: PointF) {
                    rotate(
                        angle,
                        pivot
                    )
                }
            }
        )
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        return rotationDetector.detect(event) or
            translationDetector.detect(event) or
            scaleDetector.detect(event) or
            super.onTouchEvent(event)
    }



    var errorImageId = 0
    var placeholderImageId: Int? = null


    fun setEnableGesture(){

    }


    fun setImageUri(imageUri: Uri){
        reset()

        state = LoadingState.LOADING

        this.imageUri = imageUri

        placeholderImageId?.let {
            super.setImageResource(it)
        }


        kotlin.runCatching {
            BufferedInputStream(context.contentResolver.openInputStream(imageUri), 4096).use { bis ->
                bis.mark(0)
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true

                    BitmapFactory.decodeStream(bis, null, this)

                    inJustDecodeBounds = false
                    inSampleSize = 1
                    bis.reset()

                    val bmp = BitmapFactory.decodeStream(bis)!!

                    onLoad(bmp)
                    state = LoadingState.COMPLETE
                }
            }
        }.getOrElse {
            state = LoadingState.ERROR
            onError(it)
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (state != LoadingState.COMPLETE) return
        imageMatrix = bmpMatrix

        println("currentScale: $currentScaleFactor")
        println("currentRotation: $currentRotation")
    }



    private fun reset(){
        scaleType = ScaleType.MATRIX
        state = LoadingState.NO_SOURCE
        imageUri = null
        super.setImageResource(0)

        currentScaleFactor = 1.0f
        currentRotation = 0.0f
        bmpMatrix.reset()

        rotationDetector.reset()
        scaleDetector.reset()
        translationDetector.reset()
    }


    override fun onLoad(bitmap: Bitmap) {
        size = Size(bitmap.width, bitmap.height)

        val scaleFactor = min(
            width.toFloat().div(bitmap.width),
            height.toFloat().div(bitmap.height)
        )

        // currentScaleFactor *= scaleFactor
        // bmpMatrix.postScale(scaleFactor, scaleFactor, 0f, 0f)

        zoom(scaleFactor, PointF(0.0f, 0.0f), animation = false)


        val yOffset = (height.toFloat() - bitmap.height * scaleFactor) / 2
        val xOffset = (width.toFloat() - bitmap.width * scaleFactor) / 2
        bmpMatrix.postTranslate(xOffset, yOffset)



        state = LoadingState.COMPLETE

        super.setImageBitmap(bitmap)
        invalidate()

        Log.d("SUCCESS", "SUCCESS!!")
    }

    override fun onError(e: Throwable) {
        Log.d("ERROR", "ERROR!!!", e)
    }

    init {
        reset()
    }
}
