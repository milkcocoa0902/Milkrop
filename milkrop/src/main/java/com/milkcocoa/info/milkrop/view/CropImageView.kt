package com.milkcocoa.info.milkrop.view

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.milkcocoa.info.milkrop.util.MathExtension.minus
import kotlin.math.abs
import kotlin.math.min

/**
 * CropImageView
 * @author keita
 * @since 2023/09/21 22:43
 */

/**
 *
 */
class CropImageView: FrameLayout {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr )


    private var gestureImageView: GestureImageView? = null


    fun setImageUri(imageUri: Uri){
        gestureImageView?.setImageUri(imageUri)
    }

    init {
        val maskView = CropMaskView(context).apply {
            setAspectRatio(CropMaskView.AspectRatio(4f, 3f))
        }
        gestureImageView = GestureImageView(context).apply {
            setBackgroundColor(Color.parseColor("#666666"))
            setListener(object: GestureListener{
                override fun onRotate(view: GestureImageView, rotation: Float) {
                }

                override fun onScale(view: GestureImageView, scaleFactor: Float) {
                }

                override fun onTranslate(view: GestureImageView, pivot: PointF, edge: RectF) {
                }

                override fun onRelease(view: GestureImageView) {
                    val edge = view.currentBitmapBounds()
                    val center = view.currentBitmapCenter()


                    var dx = 0f
                    var dy = 0f
                    maskView.cropWindow()?.let { cropWindow ->
                        currentAngle().mod(90f).takeIf { it == 0f }?.let {
                            // 現在の角度が90度単位（つまり、縦や横きっちり）の場合
                            val scaleFactor = min(
                                edge.height().toFloat().div(cropWindow.height()),
                                edge.width().toFloat().div(cropWindow.width())
                            )

                            val windowCenter = PointF(
                                cropWindow.centerX(),
                                cropWindow.centerY()
                            )


                            if(scaleFactor < 1.0){
                                // 中心に移動して拡大する
                                windowCenter.minus(center).let {
                                    view.zoom(1.1f.div(scaleFactor), currentBitmapCenter(), true)
                                    view.translate(
                                        it.x,
                                        it.y,
                                        true
                                    )
                                }
                            }else{
                                // 単に移動する

                                if(edge.bottom < cropWindow.bottom){
                                    dy += cropWindow.bottom - edge.bottom
                                }

                                if(edge.top > cropWindow.top){
                                    dy += cropWindow.top - edge.top
                                }

                                if(edge.left > cropWindow.left){
                                    dx += cropWindow.left - edge.left
                                }

                                if(edge.right < cropWindow.right){
                                    dx += cropWindow.right - edge.right
                                }

                                view.translate(
                                    dx = dx.div(1.0f),
                                    dy = dy.div(1.0f),
                                    animation = true
                                )
                            }
                        } ?: kotlin.run {
                            // テント直線の距離もとめてなんやかんや


                        }
                    }

                }

                override fun onLoadComplete() {

                }
            })
        }
        addView(gestureImageView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(maskView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    }
}
