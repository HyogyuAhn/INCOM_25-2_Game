package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.TimeUtils
import com.club.ClubGame
import com.club.ScreenId
import com.club.ui.UIComponents
import com.club.utils.Rng
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ReflexScreen(game: ClubGame) : BaseScreen(game) {
    private enum class GameState { PLAYING, GAME_OVER }
    private enum class CycleState { WAITING, ACTIVE }

    private val TOTAL_TIME = 60f
    private val PRESTART_DELAY = 1.0f
    private val WAIT_MIN = 0.5f
    private val WAIT_MAX = 1.5f

    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    private var scoreLabel: Label? = null
    private var comboLabel: Label? = null
    private var timeLabel: Label? = null
    private var messageLabel: Label? = null
    private var messageTable: Table? = null

    private val rows = 4
    private val cols = 4
    private lateinit var tiles: Array<Array<Rectangle>>

    private var rng = Rng()
    private var gameState = GameState.PLAYING
    private var cycleState = CycleState.WAITING
    private var timeLeft = TOTAL_TIME
    private var waitLeft = PRESTART_DELAY

    private var activeX = -1
    private var activeY = -1
    private var lastActiveIndex = -1
    private var activeMs = 900
    private var activeStartNs: Long = 0L
    private var misclicksThisCycle = 0
    private var resolvedThisCycle = false

    private var feedbackRect: Rectangle? = null
    private var feedbackTimer = 0f

    private var score = 0
    private var combo = 0
    private var maxCombo = 0
    private var correctCount = 0
    private var misclickCount = 0
    private var sumReactionMs = 0L
    private var reactionCount = 0

    private var waitSampleCount = 0
    private var waitSampleSum = 0f

    override fun setupUI() {
        setupGrid()
        setupHUD()
    }

    private fun setupGrid() {
        tiles = Array(rows) { Array(cols) { Rectangle() } }
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val gridSize = min(screenWidth * 0.8f, screenHeight * 0.8f)
        val tileSize = gridSize / cols
        val pad = tileSize * 0.08f
        val cell = tileSize - pad
        val startX = (screenWidth - gridSize) / 2f
        val startY = (screenHeight - gridSize) / 2f
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                tiles[y][x] = Rectangle(
                    startX + x * tileSize + pad / 2f,
                    startY + y * tileSize + pad / 2f,
                    cell,
                    cell
                )
            }
        }
    }

    private fun setupHUD() {
        val hudTable = Table().apply {
            setFillParent(true)
            top().left()
            pad(20f)
        }
        scoreLabel = UIComponents.createSubtitleLabel("점수: $score", game.font)
        comboLabel = UIComponents.createSubtitleLabel("콤보: $combo", game.font)
        hudTable.add(scoreLabel).left().padBottom(6f).row()
        hudTable.add(comboLabel).left().padBottom(6f).row()
        stage.addActor(hudTable)

        messageLabel = UIComponents.createTitleLabel("", game.font).apply {
            setAlignment(Align.center)
            setFontScale(1.5f)
            isVisible = false
            color = Color.WHITE
        }
        messageTable = Table().apply {
            setFillParent(true)
            center()
            add(messageLabel)
        }
        stage.addActor(messageTable)
    }

    private fun updateHUD() {
        scoreLabel?.setText("점수: $score")
        comboLabel?.setText("콤보: $combo")
    }

    override fun update(dt: Float) {
        when (gameState) {
            GameState.PLAYING -> {
                timeLeft -= dt
                if (timeLeft <= 0f) {
                    gameOver()
                    return
                }

                if (feedbackTimer > 0f) {
                    feedbackTimer -= dt
                    if (feedbackTimer <= 0f) feedbackRect = null
                }

                when (cycleState) {
                    CycleState.WAITING -> {
                        waitLeft -= dt
                        if (waitLeft <= 0f) activateNext()
                    }
                    CycleState.ACTIVE -> {
                        val elapsedMs = ((TimeUtils.nanoTime() - activeStartNs) / 1_000_000.0).toInt()
                        if (elapsedMs >= activeMs) {
                            combo = 0
                            cycleToWaiting()
                        }
                    }
                }

                handleInput()
                updateHUD()
            }
            GameState.GAME_OVER -> { }
        }
    }

    override fun handleCommonInput() { }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()

        if (cycleState != CycleState.ACTIVE) return
        if (resolvedThisCycle) return

        var hitX = -1
        var hitY = -1
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (tiles[y][x].contains(touchX, touchY)) {
                    hitX = x
                    hitY = y
                    break
                }
            }
            if (hitX != -1) break
        }
        if (hitX == -1) return

        if (hitX == activeX && hitY == activeY) {
            val nowNs = TimeUtils.nanoTime()
            val reactionMs = ceil((nowNs - activeStartNs) / 1_000_000.0).toInt().coerceAtLeast(0)
            onCorrect(reactionMs)
        } else {
            misclicksThisCycle += 1
            misclickCount += 1
            if (misclicksThisCycle >= 2) {
                combo = 0
                cycleToWaiting()
            }
        }
    }

    private fun activateNext() {
        val elapsed = TOTAL_TIME - timeLeft
        val ms = 900.0 - 600.0 * (elapsed / TOTAL_TIME)
        activeMs = ms.roundToInt().coerceIn(300, 900)

        val total = rows * cols
        var idx: Int
        do {
            idx = rng.nextInt(total)
        } while (total > 1 && idx == lastActiveIndex)
        activeY = idx / cols
        activeX = idx % cols

        misclicksThisCycle = 0
        resolvedThisCycle = false
        activeStartNs = TimeUtils.nanoTime()
        cycleState = CycleState.ACTIVE
    }

    private fun cycleToWaiting() {
        lastActiveIndex = if (activeX >= 0 && activeY >= 0) activeY * cols + activeX else lastActiveIndex
        activeX = -1
        activeY = -1
        resolvedThisCycle = true
        cycleState = CycleState.WAITING
        waitLeft = sampleWait()
    }

    private fun sampleWait(): Float {
        val avg = if (waitSampleCount == 0) 1.0f else waitSampleSum / waitSampleCount
        var low = WAIT_MIN
        var high = WAIT_MAX
        val delta = avg - 1.0f
        if (abs(delta) > 0.001f) {
            val eps = min(0.25f, abs(delta) * 0.5f)
            if (delta < 0f) {
                low = (low + eps).coerceAtMost(high - 0.05f)
            } else {
                high = (high - eps).coerceAtLeast(low + 0.05f)
            }
        }
        val sampled = rng.nextFloat(low, high)
        waitSampleSum += sampled
        waitSampleCount += 1
        return sampled
    }

    private fun onCorrect(reactionMs: Int) {
        if (resolvedThisCycle) return
        resolvedThisCycle = true

        val base = when {
            reactionMs >= 700 -> 1
            reactionMs >= 500 -> 2
            reactionMs >= 350 -> 3
            reactionMs >= 200 -> 4
            else -> 5
        }

        val keepCombo = reactionMs <= (activeMs / 2)
        combo = if (keepCombo) combo + 1 else 0
        maxCombo = max(maxCombo, combo)
        val comboBonus = when {
            combo >= 10 -> 3
            combo >= 5 -> 1
            else -> 0
        }

        val speedBonus = when {
            reactionMs <= 10 -> 10
            reactionMs <= 30 -> 3
            reactionMs <= 50 -> 2
            reactionMs <= 100 -> 1
            else -> 0
        }

        val gained = base + comboBonus + speedBonus
        score += gained

        correctCount += 1
        reactionCount += 1
        sumReactionMs += reactionMs.toLong()

        val rect = tiles[activeY][activeX]
        feedbackRect = rect
        feedbackTimer = 0.05f

        Gdx.app.log(
            "Reflex",
            "correct r=${reactionMs}ms active=${activeMs}ms base=$base combo=$combo(+${comboBonus}) speed=+${speedBonus} total+=$gained score=$score"
        )

        cycleToWaiting()
    }

    private fun gameOver() {
        val avgMs = if (reactionCount > 0) (sumReactionMs.toDouble() / reactionCount).roundToInt() else 0
        Gdx.app.log("Reflex", "Game Over score=$score maxCombo=$maxCombo avg=${avgMs}ms correct=$correctCount miss=$misclickCount")
        game.router.goToReflexGameOver(score)
    }

    override fun draw(alpha: Float) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.projectionMatrix = game.spriteBatch.projectionMatrix
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        val colorBlind = game.settings.colorBlindMode
        val inactive = if (colorBlind) Color(0.22f, 0.22f, 0.22f, 1f) else Color(0.35f, 0.37f, 0.42f, 1f)
        val active = if (colorBlind) Color(0.95f, 0.95f, 0.2f, 1f) else Color(0.20f, 0.80f, 0.30f, 1f)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val rect = tiles[y][x]
                val isActive = (x == activeX && y == activeY && cycleState == CycleState.ACTIVE)
                shapeRenderer.setColor(if (isActive) active else inactive)
                shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
            }
        }

        if (feedbackRect != null && feedbackTimer > 0f) {
            val r = feedbackRect!!
            shapeRenderer.setColor(1f, 1f, 1f, 0.6f)
            shapeRenderer.rect(r.x, r.y, r.width, r.height)
        }

        shapeRenderer.end()
        if (colorBlind && cycleState == CycleState.ACTIVE && activeX >= 0 && activeY >= 0) {
            val r = tiles[activeY][activeX]
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.setColor(Color.WHITE)
            for (i in 0..2) {
                shapeRenderer.rect(r.x - i, r.y - i, r.width + 2 * i, r.height + 2 * i)
            }
            shapeRenderer.end()
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        } else {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        }

        val barWidth = Gdx.graphics.width * 0.6f
        val barHeight = 16f
        val barX = (Gdx.graphics.width - barWidth) / 2f
        val barY = Gdx.graphics.height - 40f
        val ratio = (timeLeft / TOTAL_TIME).coerceIn(0f, 1f)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.35f))
        shapeRenderer.rect(barX, barY, barWidth, barHeight)
        shapeRenderer.setColor(Color(0.2f, 0.8f, 0.3f, 1f))
        shapeRenderer.rect(barX, barY, barWidth * ratio, barHeight)

        shapeRenderer.end()

        val timeText = "${ceil(timeLeft).toInt()}초"
        layout.setText(game.font, timeText)
        val oldProj = game.spriteBatch.projectionMatrix.cpy()
        game.spriteBatch.projectionMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        game.spriteBatch.begin()
        val textX = barX + barWidth + 10f
        val textY = barY + (barHeight + layout.height) / 2f
        game.font.draw(game.spriteBatch, layout, textX, textY)
        game.spriteBatch.end()
        game.spriteBatch.projectionMatrix = oldProj
    }

    override fun dispose() {
        super.dispose()
        shapeRenderer.dispose()
    }
}
