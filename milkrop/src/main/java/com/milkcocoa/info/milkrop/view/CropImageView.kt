package com.milkcocoa.info.milkrop.view

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout

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
            setListener(object: GestureListener{
                override fun onRotate(view: GestureImageView, rotation: Float) {
                }

                override fun onScale(view: GestureImageView, scaleFactor: Float) {
                }

                override fun onTranslate(view: GestureImageView, pivot: PointF, edge: RectF) {
                }

                override fun onRelease(view: GestureImageView) {
                    val pivot = view.currentBitmapCenter()
                    val factor = view.currentScale()
                    val size = view.getBitmapSize()
                    val edge = view.currentBitmapCorners()

                    maskView.cropWindow()?.let { cropWindow ->

                        var dx = 0f
                        var dy = 0f

                        if(edge.bottom < cropWindow.bottom){
                            dy += -(edge.bottom - cropWindow.bottom)
                        }

                        if(edge.top > cropWindow.top){
                            dy += -(edge.top - cropWindow.top)
                        }

                        if(edge.left > cropWindow.left){
                            dx += -(edge.left - cropWindow.left)
                        }
                        if(edge.right < cropWindow.right){
                            dx += -(edge.right - cropWindow.right)
                        }

                        view.translate(
                            dx = dx.div(1.0f),
                            dy = dy.div(1.0f),
                            animation = false
                        )
                    }

                }
            })
        }
        addView(gestureImageView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(maskView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    }
}
