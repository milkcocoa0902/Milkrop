package com.milkcocoa.info.milkrop.view

import android.content.Context
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
        gestureImageView = GestureImageView(context).apply {

        }
        addView(gestureImageView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}
