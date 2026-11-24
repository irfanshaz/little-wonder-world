package org.littlegrove.learn

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class TracePad(context: Activity, private val letter: String) : View(context) {
    private val strokes = mutableListOf<Path>()
    private var currentPath: Path? = null
    private val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(205, 219, 212)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 3
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(9f, 9f), 0f)
    }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 124, 115)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 12
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pathBounds = RectF()

    init {
        contentDescription = "Tracing board for the letter " + letter
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = context.resources.displayMetrics.density * 24
        }
        isFocusable = true
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height) * 0.62f
        guide.textSize = size
        canvas.drawText(letter, width / 2f, height / 2f - (guide.ascent() + guide.descent()) / 2f, guide)
        strokes.forEach { canvas.drawPath(it, ink) }
        currentPath?.let { canvas.drawPath(it, ink) }
        if (strokes.isEmpty() && currentPath == null) {
            val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(91, 112, 105)
                textSize = 15f * resources.displayMetrics.density
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Draw over the letter with one finger", width / 2f, height - 22f * resources.displayMetrics.density, hint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(event.x, event.y) }
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let { path ->
                    path.computeBounds(pathBounds, true)
                    if (pathBounds.width() + pathBounds.height() > resources.displayMetrics.density * 12f) strokes.add(path)
                }
                currentPath = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun clear() {
        strokes.clear()
        currentPath = null
        invalidate()
    }

    fun hasDrawing(): Boolean = strokes.isNotEmpty()
}
