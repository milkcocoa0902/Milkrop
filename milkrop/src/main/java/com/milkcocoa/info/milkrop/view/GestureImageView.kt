package com.milkcocoa.info.milkrop.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.SizeF
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.FloatRange
import com.milkcocoa.info.milkrop.gesture.RotationGestureDetector
import com.milkcocoa.info.milkrop.gesture.RotationGestureListener
import com.milkcocoa.info.milkrop.gesture.ScaleGestureListener
import com.milkcocoa.info.milkrop.gesture.TranslationGestureDetector
import com.milkcocoa.info.milkrop.gesture.TranslationGestureListener
import com.milkcocoa.info.milkrop.model.DistanceF
import com.milkcocoa.info.milkrop.util.MathExtension.plus
import com.milkcocoa.info.milkrop.util.MathExtension.rangeOut
import com.milkcocoa.info.milkrop.util.MathExtension.times
import com.milkcocoa.info.milkrop.util.MatrixExtension.get
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

interface ImageListener{
    fun onError(e: Throwable)
    fun onLoad(bitmap: Bitmap)
}

interface GestureListener{
    fun onTranslate(view: GestureImageView, pivot: PointF, edge: RectF)
    fun onRotate(view: GestureImageView,rotation: Float)
    fun onScale(view: GestureImageView,scaleFactor: Float)

    fun onRelease(view: GestureImageView)
}

class GestureImageView: androidx.appcompat.widget.AppCompatImageView, ImageListener {



    companion object{
        private const val EPS = 1e-4
    }

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr )

    private var listener: GestureListener? = null
    fun setListener(listener: GestureListener?){
        this.listener = listener
    }




    enum class LoadingState{
        NO_SOURCE,
        LOADING,
        COMPLETE,
        ERROR,
    }
    private var state = LoadingState.NO_SOURCE

    fun getMatrixScale(matrix: Matrix): Float{
        return sqrt(
            matrix.get(Matrix.MSCALE_X) .toDouble().pow(2.0)
                + matrix.get(Matrix.MSKEW_Y).toDouble().pow(2.0)
        ).toFloat()
    }

    fun getMatrixAngle(matrix: Matrix): Float{
        return (atan2(
            matrix.get(Matrix.MSKEW_X),
            matrix.get(Matrix.MSCALE_X)
        ) * (180.0 / Math.PI)).unaryMinus().toFloat()
    }



    private var size: SizeF = SizeF(0f, 0f)
    public fun getBitmapSize() = size


    private var imageUri: Uri? = null

    private val currentImageMatrix = Matrix()




    fun currentAngle() = getMatrixAngle(currentImageMatrix)
    fun currentScale() = getMatrixScale(currentImageMatrix)


    fun currentTranslate(): DistanceF{
        val v = FloatArray(9)
        currentImageMatrix.getValues(v)

        return DistanceF(
            v[Matrix.MTRANS_X],
            v[Matrix.MTRANS_Y]
        )
    }

    fun currentBitmapCenter(): PointF{
        val sz = getBitmapSize().times(0.5f)
        val translate = currentTranslate()
        val factor = currentScale()

        return PointF(x, y)
            .plus(translate)
            .plus(sz.times(factor).let { DistanceF(it.width, it.height) })

    }

    fun currentBitmapCorners(): RectF{
        val scale = currentScale()
        val translate = currentTranslate()

        return RectF(
            translate.x,
            translate.y,
            translate.x.plus(size.width.times(scale)),
            translate.y.plus(size.height.times(scale))
        )
    }


    private var _maxScale: Float = 3.5f
    fun setMaxScale(
        @FloatRange(from = 0.0)
        value: Float,
    ){
        _maxScale = value
        if(currentScale() < _maxScale){
            zoom(maxScale, animation = false)
        }
    }
    public val maxScale get() = _maxScale


    private var _minScale: Float = 0.2f
    fun setMinScale(
        @FloatRange(from = 0.0)
        value: Float,
    ){
        _maxScale = value
        if(currentScale() > _minScale){
            zoom(_minScale, animation = false)
        }
    }
    public val minScale get() = _minScale

    override fun getRotation(): Float {
        val v = FloatArray(9)
        currentImageMatrix.getValues(v)

        return (atan2(
            v[Matrix.MSKEW_X].toDouble(),
            v[Matrix.MSCALE_X].toDouble()
        ) * (180 / Math.PI)).roundToInt().toFloat()
    }

    override fun setRotation(rot: Float){
        rotate(rot - rotation)
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
                    translate(-distanceX, -distanceY)
                }

                override fun onDoubleTap(touchPoint: PointF) {
                    zoom(
                        scaleFactor = 1.2f,
                        pivot = touchPoint,
                        animation = false
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
        pivot: PointF,
    ){
        currentImageMatrix.postRotate(
            rotation,
            pivot.x,
            pivot.y
        )
        invalidate()

        listener?.onRotate(
            view = this@GestureImageView,
            rotation = currentAngle()
        )
    }




    /**
     * @param scaleFactor[Float] scale to multiply
     * @param animation[Boolean] do animation. if false, scaling immediately, false scaling with animation. Note that if use animation, last scale IS NOT match specified, but similar.
     */
    fun zoom(
        @FloatRange(from = 0.0)
        scaleFactor: Float,
        animation: Boolean,
    ){
        zoom(
            scaleFactor,
            currentBitmapCenter(),
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
        animation: Boolean,
    ){
        if(currentScale().rangeOut(minScale, maxScale, EPS)){
            return
        }

        if(animation){
            val interpolator = AccelerateDecelerateInterpolator()
            val startTime = System.currentTimeMillis()
            val duration = 250L


            val targetScaleFactor = (currentScale() * scaleFactor).coerceIn(minScale, maxScale)
            val initialScaleFactor = currentScale()
            var lastScaleFactor = initialScaleFactor


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


                    currentImageMatrix.postScale(tmpScaleFactor, tmpScaleFactor, pivot.x, pivot.y)

                    invalidate()
                    if(t < 1.0f){
                        post(this)
                    }else{
                        initialScaleFactor.times(realScaleFactor).coerceIn(minScale, maxScale).div(targetScaleFactor).let { errorFactor ->
                            currentImageMatrix.postScale(
                                errorFactor,
                                errorFactor,
                                pivot.x,
                                pivot.y
                            )
                            invalidate()

                            listener?.onScale(
                                view = this@GestureImageView,
                                scaleFactor = currentScale()
                            )

                        }
                    }
                }
            })
        }else{
            val limitedScaleFactor =
                currentScale().times(scaleFactor)
                    .coerceIn(minScale, maxScale)
                    .div(currentScale())

            currentImageMatrix.postScale(
                limitedScaleFactor,
                limitedScaleFactor,
                pivot.x,
                pivot.y
            )
            invalidate()


            listener?.onScale(
                view = this@GestureImageView,
                scaleFactor = currentScale()
            )
        }
    }


    fun translate(dx: Float, dy: Float, animation: Boolean = false){
        if(animation){
            val interpolator = AccelerateDecelerateInterpolator()
            val startTime = System.currentTimeMillis()
            val duration = 250L

            var totalTranslationX = 0f
            var totalTranslationY = 0f


            post(object: Runnable{
                override fun run() {
                    val t = (System.currentTimeMillis() - startTime).toFloat().div(duration).coerceAtMost(1.0f)

                    val interpolatorRatio = interpolator.getInterpolation(t)
                    val tmpTranslationX = interpolatorRatio.times(dx) - totalTranslationX
                    val tmpTranslationY = interpolatorRatio.times(dy) - totalTranslationY
                    currentImageMatrix.postTranslate(tmpTranslationX, tmpTranslationY)

                    totalTranslationX += tmpTranslationX
                    totalTranslationY += tmpTranslationY

                    invalidate()

                    if(t < 1.0f){
                        post(this)
                    }else{
                        val errorTranslate = DistanceF(
                            dx - totalTranslationX,
                            dy - totalTranslationY
                        )
                        currentImageMatrix.postTranslate(
                            errorTranslate.x,
                            errorTranslate.y
                        )
                        invalidate()

                        listener?.onTranslate(
                            view = this@GestureImageView,
                            pivot = currentBitmapCenter(),
                            edge = currentBitmapCorners()
                        )
                    }
                }
            })
        }else{
            currentImageMatrix.postTranslate(dx, dy)
            invalidate()
            listener?.onTranslate(
                view = this@GestureImageView,
                pivot = currentBitmapCenter(),
                edge =currentBitmapCorners()
            )
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

        when(event.action){
            MotionEvent.ACTION_UP ->{
                listener?.onRelease(this@GestureImageView)
            }
        }

        return rotationDetector.detect(event) or
            translationDetector.detect(event) or
            scaleDetector.detect(event) or
            super.onTouchEvent(event)
    }



    var errorImageId = 0
    var placeholderImageId: Int? = null


    fun setEnableGesture(){

    }

    override fun setImageBitmap(bm: Bitmap?) {
        reset()

        state = LoadingState.LOADING
        this.imageUri = null

        bm?.let { onLoad(it) }
    }


    fun setImageUri(imageUri: Uri){
        reset()

        state = LoadingState.LOADING

        this.imageUri = imageUri

        placeholderImageId?.let {
            super.setImageResource(it)
        }


        kotlin.runCatching {
            BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(imageUri)?.buffered(8192),
                    null,
                    this
                )

                inJustDecodeBounds = false
                inSampleSize = 1


                BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(imageUri)?.buffered(8192),
                    null,
                    this
                )?.apply {
                    onLoad(this)
                    state = LoadingState.COMPLETE
                } ?: kotlin.run {
                    state = LoadingState.ERROR
                    onError(IOException())
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
        imageMatrix = currentImageMatrix


        println("scale: $scaleX, translate: ${currentTranslate()}, scale: ${currentScale()}, pivot: ${currentBitmapCenter()}")
    }



    private fun reset(){
        scaleType = ScaleType.MATRIX
        state = LoadingState.NO_SOURCE
        imageUri = null
        super.setImageResource(0)

        currentImageMatrix.reset()

        rotationDetector.reset()
        scaleDetector.reset()
        translationDetector.reset()
    }

    var initialImageCorners = floatArrayOf()
    var initialImageCenter = floatArrayOf()

    override fun onLoad(bitmap: Bitmap) {
        super.setImageBitmap(bitmap)

        drawable ?: return


        size = SizeF(bitmap.width.toFloat(), bitmap.height.toFloat())

        val scaleFactor = min(
            width.toFloat().div(bitmap.width),
            height.toFloat().div(bitmap.height)
        )

        zoom(scaleFactor, PointF(0f, 0f), animation = false)

        val yOffset = (height.toFloat() - bitmap.height * scaleFactor) / 2
        val xOffset = (width.toFloat() - bitmap.width * scaleFactor) / 2
        translate(
            xOffset,
            yOffset,
            false
        )


        state = LoadingState.COMPLETE

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
