package com.example.parallaxgame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.random.Random

class Obstacle(context: Context, resId: Int, private val screenW: Int, private val screenH: Int) {
    private val bmp: Bitmap
    private var x: Float = 0f
    private var y: Float = 0f
    private var speed: Float = 450f  // Tăng tốc độ từ 350f lên 450f
    private var spacing: Int = 600   // Khoảng cách ban đầu giữa các chướng ngại vật

    // Thêm biến để theo dõi trạng thái đã vượt qua
    private var passed = false

    // Vị trí của nhân vật (mặc định ở 1/4 màn hình)
    private val playerPositionX = screenW * 0.25f

    private val bounds = RectF()

    // Getter cho biến passed
    fun isPassed(): Boolean = passed

    // Setter để đánh dấu đã vượt qua
    fun setPassed(value: Boolean) {
        passed = value
    }

    val width: Float
        get() = bmp.width.toFloat()

    val height: Float
        get() = bmp.height.toFloat()

    // Lấy tọa độ X của chướng ngại vật
    fun getX(): Float = x

    init {
        val raw = BitmapFactory.decodeResource(context.resources, resId)
        val scale = (screenH * 0.05f) / raw.height  // Giảm kích thước từ 8% xuống 5% màn hình

        // Tạo chướng ngại vật ngắn hơn bằng cách điều chỉnh chiều rộng của nó
        val w = (raw.width * scale * 0.7f).toInt()  // Giảm chiều rộng xuống 70%
        val h = (raw.height * scale).toInt()

        bmp = Bitmap.createScaledBitmap(raw, w, h, true)
        raw.recycle()

        // Đặt chướng ngại vật ở bên phải màn hình
        x = screenW.toFloat() + Random.nextInt(0, 500)
        // Đặt chướng ngại vật trên mặt đất
        y = (screenH * 0.8f) - bmp.height

        updateBounds()
    }

    fun update(dt: Float) {
        // Di chuyển chướng ngại vật từ phải qua trái
        x -= speed * dt

        // Nếu chướng ngại vật ra khỏi màn hình bên trái, đặt lại vị trí ở bên phải
        if (x < -bmp.width) {
            reset()
        }

        // Cập nhật hình chữ nhật va chạm
        updateBounds()
    }

    // Thêm phương thức để điều chỉnh tốc độ
    fun setSpeed(newSpeed: Float) {
        speed = newSpeed
    }

    // Thêm phương thức để điều chỉnh khoảng cách
    fun setSpacing(newSpacing: Int) {
        spacing = newSpacing
    }

    // Thêm phương thức để kiểm tra xem nhân vật đã vượt qua chướng ngại vật chưa
    fun checkIfPassed(): Boolean {
        // Nếu chướng ngại vật đã được đánh dấu là vượt qua, không làm gì cả
        if (passed) return false

        // Nếu mép phải của chướng ngại vật đã đi qua vị trí của nhân vật
        // và chưa được đánh dấu là đã vượt qua
        if (x + width < playerPositionX) {
            passed = true
            return true
        }
        return false
    }

    private fun updateBounds() {
        val insetX = bmp.width * 0.2f
        val insetY = bmp.height * 0.2f
        val left = x + insetX
        val top = y + insetY
        val right = x + bmp.width - insetX
        val bottom = y + bmp.height - insetY

        bounds.set(
            left,
            top,
            if (right > left) right else left,
            if (bottom > top) bottom else top
        )
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bmp, x, y, null)
    }

    fun reset() {
        // Đặt lại vị trí chướng ngại vật ở bên phải màn hình
        // Sử dụng spacing để điều chỉnh khoảng cách
        x = screenW + Random.nextInt(spacing, spacing + 300).toFloat()
        // Đặt chướng ngại vật trên mặt đất
        y = (screenH * 0.8f) - bmp.height
        // Đặt lại trạng thái đã vượt qua
        passed = false
        updateBounds()
    }

    // Hàm kiểm tra va chạm với người chơi
    fun collidesWith(player: Player): Boolean {
        return RectF.intersects(bounds, player.getBounds())
    }
}
