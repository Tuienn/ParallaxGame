package com.example.parallaxgame

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    // Change from val to var to allow recreating the thread
    private var thread: GameThread

    // Màn hình
    private var screenW = 0
    private var screenH = 0

    // Lớp nền parallax
    private lateinit var bgFar: ParallaxLayer
    private lateinit var bgMid: ParallaxLayer

    // Người chơi
    private lateinit var player: Player

    // Danh sách chướng ngại vật
    private val obstacles = mutableListOf<Obstacle>()

    // Số lượng chướng ngại vật
    private val obstacleCount = 6  // Tăng từ 3 lên 6 chướng ngại vật

    // Biến để theo dõi điểm nhảy qua chướng ngại vật
    private var obstacleScore = 0

    // Điểm số
    private var score = 0

    // Hệ thống độ khó tăng dần
    private var currentDifficulty = 1 // Cấp độ khó bắt đầu từ 1
    private val difficultyThresholds = listOf(0, 500, 1000, 2000, 3500, 5000) // Ngưỡng điểm để tăng độ khó
    private val difficultySpeedFactors = listOf(0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f) // Hệ số tốc độ theo cấp độ
    private val baseObstacleSpeed = 300f // Tốc độ cơ bản của chướng ngại vật

    // Khoảng cách giữa các chướng ngại vật theo cấp độ
    private val difficultyObstacleSpacing = listOf(1000, 800, 600, 500, 400, 350) // Khoảng cách giữa các chướng ngại vật

    // Trạng thái trò chơi
    private var gameOver = false
    private var gameStarted = false  // Trạng thái mới để kiểm tra xem trò chơi đã bắt đầu chưa

    // Trạng thái pause/setting
    private var isPaused = false
    private var isInSettings = false

    // Button Pause - Di chuyển sang phải cùng
    private val pauseButtonRect = RectF()
    private val pausePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    // Màn hình khởi động
    private val startButtonRect = RectF()
    private val startButtonPaint = Paint().apply {
        color = Color.argb(220, 50, 150, 250)  // Màu xanh dương đẹp hơn
        style = Paint.Style.FILL
    }

    private val startTitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 70f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // Màn hình pause và setting
    private val pauseScreenBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)  // Màu đen trong suốt
    }

    private val menuButtonPaint = Paint().apply {
        color = Color.argb(200, 50, 50, 200)  // Màu xanh đậm
        style = Paint.Style.FILL
    }

    private val menuTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // Rect cho các nút menu
    private val resumeButtonRect = RectF()
    private val settingsButtonRect = RectF()
    private val backButtonRect = RectF()

    // Các lựa chọn setting
    private var obstacleSpeed = 450f  // Tốc độ ban đầu của chướng ngại vật
    private var jumpHeight = -900f    // Độ cao nhảy ban đầu

    // Rect cho các nút setting
    private val obstacleSpeedIncRect = RectF()
    private val obstacleSpeedDecRect = RectF()
    private val jumpHeightIncRect = RectF()
    private val jumpHeightDecRect = RectF()

    // Paint để vẽ debug/fps (tuỳ chọn)
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
    }

    // Paint để vẽ điểm số
    private val scorePaint = Paint().apply {
        color = Color.YELLOW
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    // Paint để hiển thị thông báo game over
    private val gameOverPaint = Paint().apply {
        color = Color.RED
        textSize = 80f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        // Initialize the thread
        thread = GameThread(holder, this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            // Lấy kích thước surface
            screenW = width
            screenH = height

            // Tạo layer nền (scale ảnh theo chiều cao màn)
            bgFar = ParallaxLayer(context, R.drawable.far, speedPxPerSec = 40f, screenW, screenH)
            bgMid = ParallaxLayer(context, R.drawable.mid, speedPxPerSec = 120f, screenW, screenH)

            // Tạo player với hình ảnh mới
            player = Player(context, R.drawable.player_new, screenW, screenH)

            // Khởi tạo lại cấp độ khó
            currentDifficulty = 1

            // Tạo các chướng ngại vật với tốc độ ban đầu thấp
            obstacles.clear()
            val initialSpeed = baseObstacleSpeed * difficultySpeedFactors[currentDifficulty]
            val initialSpacing = difficultyObstacleSpacing[currentDifficulty]

            for (i in 0 until obstacleCount) {
                val obstacle = Obstacle(context, R.drawable.obstacle, screenW, screenH)
                obstacle.setSpeed(initialSpeed)
                obstacle.setSpacing(initialSpacing)
                // Đặt các chướng ngại vật cách đều nhau
                obstacle.reset()
                obstacles.add(obstacle)
            }

            // Đặt lại điểm số và trạng thái trò chơi
            score = 0
            gameOver = false

            // Start loop
            thread.running = true
            thread.start()
        } catch (e: Exception) {
            // Ghi lại lỗi và ngăn ứng dụng bị crash
            e.printStackTrace()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Dừng thread an toàn
        var retry = true
        thread.running = false
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (_: InterruptedException) {}
        }
    }

    fun update(dt: Float) {
        try {
            // Không cập nhật nếu trò chơi kết thúc hoặc đang ở trạng thái pause
            if (gameOver || isPaused || !gameStarted) return

            // Cập nhật nền
            bgFar?.update(dt)
            bgMid?.update(dt)

            // Cập nhật player
            player?.update(dt, screenW, screenH)

            // Cập nhật chướng ngại vật và kiểm tra tính điểm
            obstacles.forEach { obstacle ->
                obstacle.update(dt)

                // Kiểm tra va chạm
                if (obstacle.collidesWith(player)) {
                    gameOver = true
                }

                // Kiểm tra xem đã vượt qua chướng ngại vật chưa và cộng điểm
                if (obstacle.checkIfPassed()) {
                    // Cộng 1 điểm khi vượt qua chướng ngại vật
                    obstacleScore += 1
                    score += 1 // Cộng thêm 1 điểm vào tổng điểm
                }
            }

            // Tăng điểm theo thời gian (giảm tỷ lệ tăng điểm theo thời gian để điểm vượt chướng ngại vật có ý nghĩa hơn)
            score += (dt * 5).toInt()

            // Kiểm tra và cập nhật độ khó
            updateDifficulty()

        } catch (e: Exception) {
            // Xử lý lỗi khi cập nhật
            e.printStackTrace()
        }
    }

    private fun updateDifficulty() {
        // Kiểm tra xem điểm số có vượt qua ngưỡng để tăng độ khó không
        for (i in difficultyThresholds.size - 1 downTo currentDifficulty + 1) {
            if (score >= difficultyThresholds[i]) {
                currentDifficulty = i
                break
            }
        }

        // Cập nhật tốc độ và khoảng cách của chướng ngại vật theo cấp độ khó
        val currentSpeed = baseObstacleSpeed * difficultySpeedFactors[currentDifficulty]
        val currentSpacing = difficultyObstacleSpacing[currentDifficulty]

        // Áp dụng tốc độ và khoảng cách mới cho tất cả chướng ngại vật
        obstacles.forEach {
            it.setSpeed(currentSpeed)
            it.setSpacing(currentSpacing)
        }
    }

    // Vẽ thông tin độ khó hiện tại
    private fun drawDifficultyInfo(canvas: Canvas) {
        if (!gameOver && gameStarted) {
            val difficultyText = "Cấp độ: $currentDifficulty"
            canvas.drawText(difficultyText, 20f, 80f, textPaint)
        }
    }

    fun render(canvas: Canvas) {
        try {
            // Xoá nền
            canvas.drawColor(Color.BLACK)

            // Vẽ nền parallax từ xa đến gần
            bgFar?.draw(canvas)
            bgMid?.draw(canvas)

            // Vẽ chướng ngại vật
            obstacles.forEach { it.draw(canvas) }

            // Vẽ player
            player?.draw(canvas)

            // Vẽ điểm số
            canvas.drawText("Điểm: $score", screenW - 20f, 50f, scorePaint)

            // Vẽ thông tin cấp độ khó
            drawDifficultyInfo(canvas)

            // Bỏ dòng chữ hướng dẫn
            if (gameOver) {
                // Vẽ thông báo kết thúc trò chơi
                canvas.drawText("GAME OVER", screenW / 2f, screenH / 2f, gameOverPaint)
                canvas.drawText("Đạt cấp độ: $currentDifficulty", screenW / 2f, screenH / 2f + 70f, textPaint)
                canvas.drawText("Tap để chơi lại", screenW / 2f, screenH / 2f + 140f, textPaint)
            }

            // Vẽ nút pause
            if (!gameOver && gameStarted) {
                drawPauseButton(canvas)
            }

            // Vẽ màn hình pause nếu đang ở trạng thái pause
            if (isPaused) {
                drawPauseScreen(canvas)
            }

            // Vẽ màn hình settings nếu đang ở trạng thái settings
            if (isInSettings) {
                drawSettingsScreen(canvas)
            }

            // Vẽ màn hình khởi động nếu trò chơi chưa bắt đầu
            if (!gameStarted) {
                drawStartScreen(canvas)
            }
        } catch (e: Exception) {
            // Xử lý lỗi khi vẽ
            e.printStackTrace()
            // Vẽ màn hình đen nếu có lỗi
            canvas.drawColor(Color.BLACK)
        }
    }

    private fun drawPauseButton(canvas: Canvas) {
        // Vẽ hình chữ nhật cho nút pause - Chuyển sang góc trái trên
        pauseButtonRect.set(20f, 20f, 100f, 80f)
        canvas.drawRect(pauseButtonRect, pausePaint)

        // Vẽ biểu tượng pause (2 thanh dọc)
        val barWidth = 10f
        val barMargin = 20f
        val leftBarRect = RectF(
            pauseButtonRect.left + barMargin,
            pauseButtonRect.top + barMargin,
            pauseButtonRect.left + barMargin + barWidth,
            pauseButtonRect.bottom - barMargin
        )
        val rightBarRect = RectF(
            pauseButtonRect.right - barMargin - barWidth,
            pauseButtonRect.top + barMargin,
            pauseButtonRect.right - barMargin,
            pauseButtonRect.bottom - barMargin
        )
        canvas.drawRect(leftBarRect, pausePaint)
        canvas.drawRect(rightBarRect, pausePaint)
    }

    private fun drawPauseScreen(canvas: Canvas) {
        // Vẽ nền cho màn hình pause
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), pauseScreenBgPaint)

        // Vẽ nút Resume
        resumeButtonRect.set(
            screenW / 2f - 150f,
            screenH / 2f - 100f,
            screenW / 2f + 150f,
            screenH / 2f - 50f
        )
        canvas.drawRect(resumeButtonRect, menuButtonPaint)
        canvas.drawText("Resume", screenW / 2f, screenH / 2f - 70f, menuTextPaint)

        // Vẽ nút Settings
        settingsButtonRect.set(
            screenW / 2f - 150f,
            screenH / 2f + 10f,
            screenW / 2f + 150f,
            screenH / 2f + 60f
        )
        canvas.drawRect(settingsButtonRect, menuButtonPaint)
        canvas.drawText("Settings", screenW / 2f, screenH / 2f + 40f, menuTextPaint)

        // Vẽ nút Quit (tạm dừng ở đây, có thể thêm nút Quit sau)
    }

    private fun drawSettingsScreen(canvas: Canvas) {
        // Vẽ nền cho màn hình settings
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), pauseScreenBgPaint)

        // Vẽ nút Back
        backButtonRect.set(
            20f,
            20f,
            20f + 150f,
            20f + 60f
        )
        canvas.drawRect(backButtonRect, menuButtonPaint)
        canvas.drawText("Back", 20f + 75f, 20f + 40f, menuTextPaint)

        // Vẽ các tuỳ chọn cài đặt
        val settingOptionHeight = 80f
        val optionMargin = 20f

        // Tùy chọn tốc độ chướng ngại vật
        canvas.drawText("Obstacle Speed:", screenW / 2f, screenH / 2f - settingOptionHeight, menuTextPaint)
        obstacleSpeedDecRect.set(
            screenW / 2f - 100f,
            screenH / 2f - settingOptionHeight / 2f,
            screenW / 2f - 50f,
            screenH / 2f + settingOptionHeight / 2f
        )
        canvas.drawRect(obstacleSpeedDecRect, menuButtonPaint)
        canvas.drawText("-", screenW / 2f - 75f, screenH / 2f + 10f, menuTextPaint)

        obstacleSpeedIncRect.set(
            screenW / 2f + 50f,
            screenH / 2f - settingOptionHeight / 2f,
            screenW / 2f + 100f,
            screenH / 2f + settingOptionHeight / 2f
        )
        canvas.drawRect(obstacleSpeedIncRect, menuButtonPaint)
        canvas.drawText("+", screenW / 2f + 75f, screenH / 2f + 10f, menuTextPaint)

        // Tùy chọn độ cao nhảy
        canvas.drawText("Jump Height:", screenW / 2f, screenH / 2f + optionMargin + 20f, menuTextPaint)
        jumpHeightDecRect.set(
            screenW / 2f - 100f,
            screenH / 2f + optionMargin + 20f - settingOptionHeight / 2f,
            screenW / 2f - 50f,
            screenH / 2f + optionMargin + 20f + settingOptionHeight / 2f
        )
        canvas.drawRect(jumpHeightDecRect, menuButtonPaint)
        canvas.drawText("-", screenW / 2f - 75f, screenH / 2f + optionMargin + 30f, menuTextPaint)

        jumpHeightIncRect.set(
            screenW / 2f + 50f,
            screenH / 2f + optionMargin + 20f - settingOptionHeight / 2f,
            screenW / 2f + 100f,
            screenH / 2f + optionMargin + 20f + settingOptionHeight / 2f
        )
        canvas.drawRect(jumpHeightIncRect, menuButtonPaint)
        canvas.drawText("+", screenW / 2f + 75f, screenH / 2f + optionMargin + 30f, menuTextPaint)
    }

    private fun drawStartScreen(canvas: Canvas) {
        // Vẽ nền cho màn hình khởi động
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), pauseScreenBgPaint)

        // Vẽ tiêu đề trò chơi
        canvas.drawText("Parallax Game", screenW / 2f, screenH / 2f - 100f, startTitlePaint)

        // Vẽ nút Bắt đầu
        startButtonRect.set(
            screenW / 2f - 150f,
            screenH / 2f,
            screenW / 2f + 150f,
            screenH / 2f + 50f
        )
        canvas.drawRect(startButtonRect, startButtonPaint)
        canvas.drawText("Bắt đầu", screenW / 2f, screenH / 2f + 10f, menuTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            // Khởi động lại trò chơi nếu đã kết thúc
            if (gameOver && event.actionMasked == MotionEvent.ACTION_DOWN) {
                resetGame()
                return true
            }

            // Xử lý sự kiện chạm cho màn hình pause và settings
            if (isPaused) {
                handlePauseTouch(event)
            } else if (isInSettings) {
                handleSettingsTouch(event)
            } else if (!gameStarted) {
                // Xử lý sự kiện chạm cho màn hình khởi động
                handleStartTouch(event)
            } else {
                // Xử lý sự kiện chạm cho trò chơi bình thường
                handleGameTouch(event)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private fun handleGameTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Kiểm tra nếu chạm vào nút Pause
                if (pauseButtonRect.contains(x, y)) {
                    isPaused = true
                    return
                }

                // Tap nửa trên màn hình để nhảy
                if (y < screenH / 2 && ::player.isInitialized) {
                    player.jump()
                } else if (::player.isInitialized) {
                    // Nửa dưới màn hình để di chuyển trái/phải
                    player.handleTouch(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                // Điều khiển di chuyển
                if (::player.isInitialized) {
                    player.handleTouch(x, y)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (::player.isInitialized) {
                    player.stopHorizontal()
                }
            }
        }
    }

    private fun handlePauseTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Kiểm tra nếu chạm vào nút Resume
                if (resumeButtonRect.contains(x, y)) {
                    isPaused = false
                } else if (settingsButtonRect.contains(x, y)) {
                    // Chuyển đến màn hình settings nếu chạm vào nút Settings
                    isInSettings = true
                    isPaused = false // Tắt trạng thái pause
                }
            }
        }
    }

    private fun handleSettingsTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Kiểm tra nếu chạm vào nút Quay lại
                if (backButtonRect.contains(x, y)) {
                    isInSettings = false
                }

                // Tăng/giảm tốc độ chướng ngại vật
                if (y < screenH / 2) {
                    // Tăng tốc độ
                    obstacleSpeed += 50f
                } else {
                    // Giảm tốc độ
                    obstacleSpeed -= 50f
                }

                // Tăng/giảm độ cao nhảy
                if (x < screenW / 2) {
                    // Tăng độ cao nhảy
                    jumpHeight += 50f
                } else {
                    // Giảm độ cao nhảy
                    jumpHeight -= 50f
                }
            }
        }
    }

    private fun handleStartTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Kiểm tra nếu chạm vào nút Bắt đầu
                if (startButtonRect.contains(x, y)) {
                    gameStarted = true
                    isPaused = false
                }
            }
        }
    }

    // Đặt lại trò chơi
    private fun resetGame() {
        score = 0
        gameOver = false
        currentDifficulty = 1  // Đặt lại cấp độ khó về mức dễ nhất

        // Đặt lại vị trí người chơi
        if (::player.isInitialized) {
            player = Player(context, R.drawable.player_new, screenW, screenH)
        }

        // Đặt lại vị trí và tốc độ chướng ngại vật
        val initialSpeed = baseObstacleSpeed * difficultySpeedFactors[currentDifficulty]
        val initialSpacing = difficultyObstacleSpacing[currentDifficulty]

        obstacles.forEach {
            it.setSpeed(initialSpeed)
            it.setSpacing(initialSpacing)
            it.reset()
        }
    }

    // Thêm hàm pause và resume để MainActivity có thể gọi
    fun pause() {
        if (thread.isAlive) {
            thread.running = false
            var retry = true
            while (retry) {
                try {
                    thread.join()
                    retry = false
                } catch (e: InterruptedException) {}
            }
        }
    }

    fun resume() {
        if (thread.isAlive && !thread.running) {
            // If thread exists but not running, just set it to running
            thread.running = true
        } else if (!thread.isAlive) {
            // If thread doesn't exist or isn't alive, create a new one
            thread = GameThread(holder, this)
            thread.running = true
            thread.start()
        }
    }
}
