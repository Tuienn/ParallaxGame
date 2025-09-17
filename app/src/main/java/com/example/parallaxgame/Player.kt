package com.example.parallaxgame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Player(context: Context, resId: Int, private val screenW: Int, private val screenH: Int) {
    private val bmp: Bitmap
    private var x = 0f
    private var y = 0f
    private var vx = 0f
    private var vy = 0f

    // Tham số vật lý đơn giản
    private val moveSpeed = 280f              // px/s
    private val jumpSpeed = -1200f            // px/s (âm là hướng lên) - tăng từ -900f lên -1200f để nhảy cao hơn
    private val gravity  = 1800f              // px/s^2 - tăng từ 1500f lên 1800f để rơi nhanh hơn
    private val friction = 900f               // px/s^2

    private val groundY: Float

    private var touchStartY = 0f
    private var lastTouchY = 0f

    // Thêm biến mới để kiểm tra trạng thái
    private var isJumping = false

    // Hình chữ nhật va chạm
    private val bounds = RectF()

    init {
        val raw = BitmapFactory.decodeResource(context.resources, resId)
        val scale = (screenH * 0.12f) / raw.height // cao ~12% màn hình
        val w = (raw.width * scale).toInt()
        val h = (raw.height * scale).toInt()
        bmp = Bitmap.createScaledBitmap(raw, w, h, true)
        raw.recycle()

        x = (screenW * 0.25f)
        groundY = (screenH * 0.8f) - bmp.height
        y = groundY

        // Khởi tạo hình chữ nhật va chạm
        updateBounds()
    }

    fun update(dt: Float, screenW: Int, screenH: Int) {
        // Trọng lực
        vy += gravity * dt

        // Ma sát khi không chạm
        if (abs(vx) > 1f) {
            val sign = if (vx > 0) 1 else -1
            vx -= sign * friction * dt
            if (vx * sign < 0) vx = 0f
        }

        // Giới hạn tốc độ ngang
        vx = max(min(vx, moveSpeed), -moveSpeed)

        // Tích phân vị trí
        x += vx * dt
        y += vy * dt

        // Cập nhật isJumping
        isJumping = y < groundY

        // Chạm đất
        if (y >= groundY) {
            y = groundY
            vy = 0f
            isJumping = false
        }

        // Chặn va biên ngang
        if (x < 0) x = 0f
        if (x > screenW - bmp.width) x = (screenW - bmp.width).toFloat()

        // Cập nhật hình chữ nhật va chạm
        updateBounds()
    }

    // Cập nhật hình chữ nhật va chạm
    private fun updateBounds() {
        bounds.set(x, y, x + bmp.width, y + bmp.height)
    }

    // Trả về hình chữ nhật va chạm
    fun getBounds(): RectF {
        return bounds
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bmp, x, y, null)
    }

    fun handleTouch(tx: Float, ty: Float) {
        // Vuốt lên để nhảy: khi mới chạm, lưu Y; di chuyển lên nhiều => nhảy
        if (touchStartY == 0f) touchStartY = ty
        lastTouchY = ty

        val isLeft = tx < screenW / 2f
        vx = if (isLeft) -moveSpeed else moveSpeed

        // Nếu kéo lên > 120px và đang ở mặt đất -> nhảy
        val swipeUp = (touchStartY - lastTouchY) > 120f
        if (swipeUp && y >= groundY - 1f) {
            vy = jumpSpeed
            isJumping = true
            // reset để không nhảy liên tục khi giữ tay
            touchStartY = lastTouchY
        }
    }

    // Thêm phương thức để nhảy bằng tap
    fun jump() {
        if (!isJumping) {
            vy = jumpSpeed
            isJumping = true
        }
    }

    fun stopHorizontal() {
        vx = 0f
        touchStartY = 0f
        lastTouchY = 0f
    }

    // Kiểm tra xem nhân vật có đang nhảy hay không
    fun isJumping(): Boolean {
        return isJumping
    }
}
