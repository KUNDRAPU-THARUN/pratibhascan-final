package com.example.prathibhascanfinal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class AnalyticsGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint().apply {
        color = "#3B82F6".toColorInt()
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        color = "#FBBF24".toColorInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = "#333B82F6".toColorInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val axisPaint = Paint().apply {
        color = "#334155".toColorInt()
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val path = android.graphics.Path()
    private val fillPath = android.graphics.Path()

    private var dataPoints: List<Float> = listOf(0.2f, 0.4f, 0.3f, 0.7f, 0.6f, 0.9f, 0.8f)

    fun setData(points: List<Float>) {
        dataPoints = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val paddingX = 60f
        val paddingY = 80f
        val w = width.toFloat() - (paddingX * 2)
        val h = height.toFloat() - (paddingY * 2)

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height - paddingY - (i * (h / gridLines))
            canvas.drawLine(paddingX, y, width - paddingX, y, axisPaint)
        }

        val stepX = w / (dataPoints.size - 1)
        
        path.reset()
        fillPath.reset()

        for (i in dataPoints.indices) {
            val x = paddingX + (i * stepX)
            val y = (height - paddingY) - (dataPoints[i] * h)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - paddingY)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if ((i == dataPoints.size - 1)) {
                fillPath.lineTo(x, height - paddingY)
                fillPath.close()
            }
        }

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        // Draw dots
        for (i in dataPoints.indices) {
            val x = paddingX + (i * stepX)
            val y = (height - paddingY) - (dataPoints[i] * h)
            canvas.drawCircle(x, y, 10f, dotPaint)
        }
    }
}
