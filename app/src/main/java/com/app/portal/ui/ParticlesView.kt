package com.app.portal.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ParticlesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var vx: Float,
        var alpha: Int,
        var color: Int
    )

    private val particles = ArrayList<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(
        Color.parseColor("#f59e0b"), // --emas-orange
        Color.parseColor("#fbbf24"), // --emas-orange-bright
        Color.parseColor("#d97706")  // --emas-orange-dark
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particles.clear()
        val count = 90
        for (i in 0 until count) {
            particles.add(createParticle(w, h, true))
        }
    }

    private fun createParticle(w: Int, h: Int, randomY: Boolean): Particle {
        val radius = (1.5f + Random.nextFloat() * 4.5f) * resources.displayMetrics.density
        val speed = (0.8f + Random.nextFloat() * 1.8f) * resources.displayMetrics.density
        val vx = (Random.nextFloat() - 0.5f) * 0.8f * resources.displayMetrics.density
        val x = Random.nextFloat() * w
        val y = if (randomY) Random.nextFloat() * h else h + 20f
        val alpha = (60 + Random.nextInt(160))
        val color = colors[Random.nextInt(colors.size)]
        return Particle(x, y, radius, speed, vx, alpha, color)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        for (p in particles) {
            p.x += p.vx
            p.y -= p.speed

            if (p.y < -20f) {
                val newP = createParticle(w, h, false)
                p.x = newP.x
                p.y = newP.y
                p.radius = newP.radius
                p.speed = newP.speed
                p.vx = newP.vx
                p.alpha = newP.alpha
                p.color = newP.color
            }

            paint.color = p.color
            paint.alpha = p.alpha
            paint.setShadowLayer(p.radius * 2f, 0f, 0f, p.color)
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }

        postInvalidateOnAnimation()
    }
}
