package com.app.portal.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object StudentStyleHelper {

    fun getStyleDrawable(
        context: Context,
        bgColor: String,
        strokeColor: String? = null,
        strokeWidthDp: Int = 0,
        radiusDp: Float = 8f,
        isGradientOrange: Boolean = false,
        isCircle: Boolean = false
    ): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            if (isCircle) {
                shape = GradientDrawable.OVAL
            } else {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusDp * density
            }

            if (isGradientOrange) {
                orientation = GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(Color.parseColor("#F59E0B"), Color.parseColor("#D97706"))
            } else if (bgColor.isNotEmpty()) {
                setColor(Color.parseColor(bgColor))
            }

            strokeColor?.let {
                setStroke((strokeWidthDp * density).toInt(), Color.parseColor(it))
            }
        }
    }

    fun applyGlowAnimation(context: Context, drawable: GradientDrawable) {
        val density = context.resources.displayMetrics.density
        ValueAnimator.ofFloat(0.3f, 0.8f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val alphaVal = ((anim.animatedValue as Float) * 255).toInt()
                val colorWithAlpha = Color.argb(alphaVal, 245, 158, 11)
                drawable.setStroke((1.5f * density).toInt(), colorWithAlpha)
            }
            start()
        }
    }
}
