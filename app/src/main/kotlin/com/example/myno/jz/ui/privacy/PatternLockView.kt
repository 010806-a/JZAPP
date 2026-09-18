package com.example.myno.jz.ui.privacy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val GRID_SIZE = 3
        private const val MIN_POINTS = 4
    }

    private val points = mutableListOf<PointF>()

    private val selectedIndexes = mutableListOf<Int>()

    private var currentX = 0f
    private var currentY = 0f

    private var isDrawing = false

    private var listener: OnPatternCompleteListener? = null

    /**
     * 未选中节点
     */
    private val normalPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.GRAY
    }

    /**
     * 已选中节点
     */
    private val selectedPointPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.FILL
        color = Color.rgb(33, 150, 243)
    }

    /**
     * 连接线
     */
    private val linePaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.rgb(33, 150, 243)
    }

    /**
     * 选中节点外圈
     */
    private val selectedRingPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.rgb(33, 150, 243)
    }

    interface OnPatternCompleteListener {
        fun onPatternComplete(pattern: String)
    }

    fun setOnPatternCompleteListener(
        listener: OnPatternCompleteListener
    ) {
        this.listener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        calculatePoints()

        drawConnectionLines(canvas)

        drawPoints(canvas)
    }

    /**
     * 计算九宫格9个节点的位置
     */
    private fun calculatePoints() {

        points.clear()

        val size = minOf(width, height)

        if (size <= 0) {
            return
        }

        val left = width / 2f - size / 2f
        val top = height / 2f - size / 2f

        val cellSize = size / 3f

        for (row in 0 until GRID_SIZE) {

            for (column in 0 until GRID_SIZE) {

                val x =
                    left +
                        column * cellSize +
                        cellSize / 2f

                val y =
                    top +
                        row * cellSize +
                        cellSize / 2f

                points.add(
                    PointF(x, y)
                )
            }
        }
    }

    /**
     * 绘制9个节点
     */
    private fun drawPoints(canvas: Canvas) {

        if (points.isEmpty()) {
            return
        }

        val radius =
            minOf(width, height) / 18f

        for (index in points.indices) {

            val point = points[index]

            if (selectedIndexes.contains(index)) {

                // 选中节点外圈
                canvas.drawCircle(
                    point.x,
                    point.y,
                    radius + 8f,
                    selectedRingPaint
                )

                // 选中节点实心
                canvas.drawCircle(
                    point.x,
                    point.y,
                    radius,
                    selectedPointPaint
                )

            } else {

                // 普通节点
                canvas.drawCircle(
                    point.x,
                    point.y,
                    radius,
                    normalPaint
                )
            }
        }
    }

    /**
     * 绘制连接线
     */
    private fun drawConnectionLines(
        canvas: Canvas
    ) {

        if (selectedIndexes.isEmpty()) {
            return
        }

        val path = Path()

        val firstPoint =
            points[selectedIndexes.first()]

        path.moveTo(
            firstPoint.x,
            firstPoint.y
        )

        for (i in 1 until selectedIndexes.size) {

            val point =
                points[selectedIndexes[i]]

            path.lineTo(
                point.x,
                point.y
            )
        }

        /**
         * 手指还在滑动时，
         * 从最后一个节点连接到手指当前位置
         */
        if (isDrawing) {

            path.lineTo(
                currentX,
                currentY
            )
        }

        canvas.drawPath(
            path,
            linePaint
        )
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                /**
                 * 非常重要：
                 * 禁止外层 ScrollView 抢走滑动事件
                 */
                parent?.requestDisallowInterceptTouchEvent(
                    true
                )

                resetPattern()

                currentX = event.x
                currentY = event.y

                val index =
                    findPoint(
                        event.x,
                        event.y
                    )

                if (index >= 0) {

                    selectedIndexes.add(index)

                    isDrawing = true

                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                /**
                 * 持续禁止 ScrollView 拦截
                 */
                parent?.requestDisallowInterceptTouchEvent(
                    true
                )

                if (!isDrawing) {
                    return true
                }

                currentX = event.x
                currentY = event.y

                val index =
                    findPoint(
                        event.x,
                        event.y
                    )

                if (
                    index >= 0 &&
                    !selectedIndexes.contains(index)
                ) {

                    selectedIndexes.add(index)

                    /**
                     * 如果刚进入新的节点，
                     * 将当前点直接吸附到节点中心
                     */
                    currentX =
                        points[index].x

                    currentY =
                        points[index].y
                }

                invalidate()

                return true
            }

            MotionEvent.ACTION_UP -> {

                parent?.requestDisallowInterceptTouchEvent(
                    false
                )

                if (!isDrawing) {
                    return true
                }

                currentX = event.x
                currentY = event.y

                isDrawing = false

                invalidate()

                if (
                    selectedIndexes.size >=
                    MIN_POINTS
                ) {

                    val pattern =
                        selectedIndexes.joinToString("-")

                    listener?.onPatternComplete(
                        pattern
                    )
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {

                parent?.requestDisallowInterceptTouchEvent(
                    false
                )

                isDrawing = false

                invalidate()

                return true
            }
        }

        return true
    }

    /**
     * 查找手指当前经过的节点
     */
    private fun findPoint(
        x: Float,
        y: Float
    ): Int {

        if (points.isEmpty()) {
            return -1
        }

        /**
         * 触摸范围扩大一点，
         * 手机上更容易操作
         */
        val touchRadius =
            minOf(width, height) / 6f

        for (index in points.indices) {

            val point =
                points[index]

            val distance =
                hypot(
                    (x - point.x).toDouble(),
                    (y - point.y).toDouble()
                )

            if (
                distance <=
                touchRadius
            ) {
                return index
            }
        }

        return -1
    }

    /**
     * 清除当前图案
     */
    fun resetPattern() {

        selectedIndexes.clear()

        isDrawing = false

        currentX = 0f
        currentY = 0f

        invalidate()
    }
}