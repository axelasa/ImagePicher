package com.example.imagepicher.Helper

import android.graphics.*

class RectOverlay internal constructor(overlay: GraphicOverlay,private val bound:Rect?):GraphicOverlay.Graphic(overlay) {

    private val rectPaint: Paint

    init {
        rectPaint = Paint()
        rectPaint.color =Color.CYAN
        rectPaint.strokeWidth = 4.0f
        rectPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas?) {
        //TODO("Not yet implemented")

        val rect = RectF(bound)
        rect.left = translateX(rect.left)
        rect.right = translateX(rect.right)
        rect.top = translateX(rect.top)
        rect.bottom = translateX(rect.bottom)

        canvas?.drawRect(rect,rectPaint)

    }

}