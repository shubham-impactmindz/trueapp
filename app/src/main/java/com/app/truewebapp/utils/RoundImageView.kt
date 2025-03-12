package com.app.truewebapp.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.app.truewebapp.R


class RoundImageView (
    context: Context,
    attributeSet: AttributeSet
) : AppCompatImageView(context,attributeSet) {

    init {
        //the outline (view edges) of the view should be derived    from the background
        outlineProvider = ViewOutlineProvider.BACKGROUND
        //cut the view to match the view to the outline of the background
        clipToOutline = true
        //use the following background to calculate the outline
        setBackgroundResource(R.drawable.bg_circle)

        //fill in the whole image view, crop if needed while keeping the center
        scaleType = ScaleType.CENTER_CROP
    }
}