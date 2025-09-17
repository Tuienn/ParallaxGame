package com.example.parallaxgame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// Lớp MainActivity là điểm vào chính của ứng dụng
class MainActivity : AppCompatActivity() {
    // Khai báo biến gameView để hiển thị trò chơi
    private lateinit var gameView: GameView

    // Hàm được gọi khi ứng dụng được tạo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo màn hình trò chơi
        gameView = GameView(this)
        // Thiết lập giao diện hiển thị
        setContentView(gameView)
    }

    // Hàm được gọi khi ứng dụng bị tạm dừng (ví dụ: người dùng chuyển sang ứng dụng khác)
    override fun onPause() {
        super.onPause()
        // Tạm dừng trò chơi
        gameView.pause()
    }

    // Hàm được gọi khi ứng dụng tiếp tục hoạt động (quay lại từ trạng thái tạm dừng)
    override fun onResume() {
        super.onResume()
        // Tiếp tục trò chơi
        gameView.resume()
    }
}
