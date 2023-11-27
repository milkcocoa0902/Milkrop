package com.milkcocoa.info.milkrop.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.milkcocoa.info.milkrop.model.CornersF

/**
 * CropMaskView
 * @author keita
 * @since 2023/11/24 22:45
 */

/**
 *
 */
class CropMaskView : View {
    data class AspectRatio(val x: Float, val y: Float)


    private val backgroundPaint = Paint()
    private val paint = Paint()

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr )


    private var ratio: AspectRatio? = null
    fun setAspectRatio(ratio: AspectRatio){
        this.ratio = ratio
        invalidate()
    }

    private var cropWindow: RectF? = null
    fun cropWindow() = cropWindow
    fun corners() = cropWindow()?.let { cw ->
        CornersF(
            lt = PointF(cw.left, cw.top),
            rt = PointF(cw.right, cw.top),
            rb = PointF(cw.right, cw.bottom),
            lb = PointF(cw.left, cw.bottom)
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        cropWindow = null

        //全体を描画する
        backgroundPaint.color = Color.parseColor("#7F000000")
        canvas.drawRect(
            0f,
            0f,
            canvas.width.toFloat(),
            canvas.height.toFloat(),
            backgroundPaint
        )

        ratio ?: return

        //全体の背景から円をくり抜く
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        val pivot = PointF(width.toFloat().div(2), height.toFloat().div(2))
        cropWindow = calculateRect(ratio!!, pivot, 20f)


        canvas.drawRect(
            cropWindow!!,
            paint
        )
    }

    private fun calculateRect(ratio: AspectRatio, pivot: PointF, margin: Float): RectF {
        val marginDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, context.applicationContext.resources.displayMetrics)
        val fullWidth = width.minus(marginDp.times(2))
        return RectF(
            pivot.x - fullWidth.div(2.0f),
            pivot.y - fullWidth.div(2.0f).times(ratio.y.div(ratio.x)),
            pivot.x + fullWidth.div(2.0f),
            pivot.y + fullWidth.div(2.0f).times(ratio.y.div(ratio.x)),
        )
    }
}
