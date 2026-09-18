package com.example.myno.jz.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CategoryDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Segment(val amount: Double, val color: Int)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(18f)
        strokeCap = Paint.Cap.BUTT
    }

    private val rect = RectF()
    private var segments: List<Segment> = emptyList()

    fun setSegments(newSegments: List<Segment>) {
        segments = newSegments.filter { it.amount > 0.0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        if (size <= 0f) return

        val inset = dp(13f)
        rect.set(inset, inset, size - inset, size - inset)

        val total = segments.sumOf { it.amount }
        if (total <= 0.0) {
            paint.color = 0xFFE8EBF1.toInt()
            canvas.drawArc(rect, -90f, 360f, false, paint)
            return
        }

        var startAngle = -90f
        segments.forEach { segment ->
            val sweep = (segment.amount / total * 360.0).toFloat()
            paint.color = segment.color
            canvas.drawArc(rect, startAngle, sweep, false, paint)
            startAngle += sweep
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
