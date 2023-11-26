package com.milkcocoa.info.milkrop.view

import android.content.Context
import android.graphics.Bitmap
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
import com.milkcocoa.info.milkrop.gesture.ScaleGestureDetector
import com.milkcocoa.info.milkrop.gesture.ScaleGestureListener
import com.milkcocoa.info.milkrop.gesture.TranslationGestureDetector
import com.milkcocoa.info.milkrop.gesture.TranslationGestureListener
import com.milkcocoa.info.milkrop.model.DistanceF
import com.milkcocoa.info.milkrop.util.BitmapLoader
import com.milkcocoa.info.milkrop.util.FastBitmapDrawable
import com.milkcocoa.info.milkrop.util.MathExtension.plus
import com.milkcocoa.info.milkrop.util.MathExtension.rangeOut
import com.milkcocoa.info.milkrop.util.MathExtension.times
import com.milkcocoa.info.milkrop.util.MatrixExtension.get
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


interface GestureListener{
    fun onTranslate(view: GestureImageView, pivot: PointF, edge: RectF)
    fun onRotate(view: GestureImageView,rotation: Float)
    fun onScale(view: GestureImageView,scaleFactor: Float)

    fun onRelease(view: GestureImageView)

    fun onLoadComplete()
}


class GestureImageView: androidx.appcompat.widget.AppCompatImageView {



    companion object{
        private const val TAG = "GestureImageView"
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

    fun currentBitmapBounds(): RectF{
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
        ) * (180 / Math.PI)).toFloat()
    }

    override fun setRotation(rot: Float){
        rotate(rot - rotation)
    }


    private val scaleDetector by lazy {
        ScaleGestureDetector(
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
                x.plus(measuredWidth.toFloat().div(2)),
                y.plus(measuredHeight.toFloat().div(2))
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
        imageMatrix = currentImageMatrix

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

                    imageMatrix = currentImageMatrix
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
                            imageMatrix = currentImageMatrix
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
            imageMatrix = currentImageMatrix


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

                    imageMatrix = currentImageMatrix

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
                        imageMatrix = currentImageMatrix

                        listener?.onTranslate(
                            view = this@GestureImageView,
                            pivot = currentBitmapCenter(),
                            edge = currentBitmapBounds()
                        )
                    }
                }
            })
        }else{
            currentImageMatrix.postTranslate(dx, dy)
            imageMatrix = currentImageMatrix
            listener?.onTranslate(
                view = this@GestureImageView,
                pivot = currentBitmapCenter(),
                edge =currentBitmapBounds()
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



    private var bitmapDecoded = false
    private var bitmapLaidOut = false
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(changed || (bitmapDecoded && bitmapLaidOut.not())){
            reset()
            fitCenterOnLoadImage()
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        state = LoadingState.LOADING
        this.imageUri = null

        bitmapDecoded = true
        bitmapLaidOut = false
        setImageDrawable(FastBitmapDrawable(bm))
    }

    fun setImageUri(imageUri: Uri){
        state = LoadingState.LOADING
        this.imageUri = imageUri
        bitmapDecoded = false
        bitmapLaidOut = false


        placeholderImageId?.let {
            super.setScaleType(ScaleType.CENTER_INSIDE)
            super.setImageResource(it)
        }

        BitmapLoader.load(context, imageUri, listener = object: BitmapLoader.BitmapLoaderListener{
            override fun onError(e: Throwable) {
                Log.d("ERROR", "ERROR!!!", e)
            }

            override suspend fun onLoad(bitmap: Bitmap) {
                bitmapDecoded = true
                bitmapLaidOut = false
                setImageDrawable(FastBitmapDrawable(bitmap))
            }
        })
    }



    private fun fitCenterOnLoadImage(){
        drawable ?: return

        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight


        size = SizeF(w.toFloat(), h.toFloat())

        val scaleFactor = min(
            width.toFloat().div(w),
            height.toFloat().div(h)
        )
        val xOffset = (width.toFloat() - w * scaleFactor) / 2
        val yOffset = (height.toFloat() - h * scaleFactor) / 2


        zoom(scaleFactor, PointF(0f, 0f), animation = false)

        translate(
            xOffset,
            yOffset,
            false
        )

        bitmapLaidOut = true

        Log.i(TAG, "image loaded with size [$w] x [$h]")
        Log.i(TAG, "image put on ${currentBitmapCenter()} with bounds ${currentBitmapBounds()} which scaled by ${currentScale()}")

        state = LoadingState.COMPLETE
        listener?.onLoadComplete()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (state != LoadingState.COMPLETE) return
        imageMatrix = currentImageMatrix


        println("scale: $scaleX, translate: ${currentTranslate()}, scale: ${currentScale()}, pivot: ${currentBitmapCenter()}")
    }

    override fun setScaleType(scaleType: ScaleType?) {

        if(scaleType == ScaleType.MATRIX){
            super.setScaleType(scaleType)
        }else{
            Log.w(TAG, "Invalid ScaleType. Only ScaleType.MATRIX can be used.")
        }
    }


    private fun reset(){
        scaleType = ScaleType.MATRIX

        currentImageMatrix.reset()

        rotationDetector.reset()
        scaleDetector.reset()
        translationDetector.reset()
    }


    init {
        state = LoadingState.NO_SOURCE
        imageUri = null
        super.setImageResource(0)

        reset()
    }
}
