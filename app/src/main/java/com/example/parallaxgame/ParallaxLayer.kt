package com.example.parallaxgame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF

class ParallaxLayer(
    context: Context,
    resId: Int,
    private val speedPxPerSec: Float,
    private val screenW: Int,
    private val screenH: Int
) {
    private val bmp: Bitmap
    private var xOffset = 0f

    init {
        val original = BitmapFactory.decodeResource(context.resources, resId)
        // Scale ảnh theo chiều cao màn hình, giữ tỉ lệ
        val scale = screenH.toFloat() / original.height
        val newW = (original.width * scale).toInt()
        bmp = Bitmap.createScaledBitmap(original, newW, screenH, true)
        original.recycle()
    }

    fun update(dt: Float) {
        xOffset -= speedPxPerSec * dt
        // Nếu đã trôi hết 1 ảnh -> cuộn vòng
        if (xOffset <= -bmp.width) xOffset += bmp.width
    }

    fun draw(canvas: Canvas) {
        // Vẽ 2 bản để che kín màn hình khi cuộn
        var drawX = xOffset
        while (drawX < screenW) {
            canvas.drawBitmap(bmp, drawX, 0f, null)
            drawX += bmp.width
        }
    }
}
