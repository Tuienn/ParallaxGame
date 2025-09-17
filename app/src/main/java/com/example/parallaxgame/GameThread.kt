package com.example.parallaxgame

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {

    @Volatile var running = false
    @Volatile private var paused = false

    private val targetFPS = 60
    private val targetTimeNs = 1_000_000_000L / targetFPS

    fun pauseLoop() { paused = true }
    fun resumeLoop() { paused = false }

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            if (paused) {
                sleep(16)
                continue
            }

            val now = System.nanoTime()
            val dt = ((now - lastTime).coerceAtMost(100_000_000L)) / 1_000_000_000f // <= 0.1s
            lastTime = now

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.update(dt)
                        gameView.render(canvas)
                    }
                }
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }

            // Chờ nếu cần để gần ~60fps
            val frameTime = System.nanoTime() - now
            val waitNs = targetTimeNs - frameTime
            if (waitNs > 0) {
                try {
                    sleep(waitNs / 1_000_000, (waitNs % 1_000_000).toInt())
                } catch (_: InterruptedException) {}
            }
        }
    }
}
