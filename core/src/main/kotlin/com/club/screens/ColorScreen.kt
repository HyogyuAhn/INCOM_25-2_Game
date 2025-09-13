package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.TimeUtils
import com.club.ClubGame
import com.club.ui.UIComponents
import com.club.utils.Rng
import kotlin.math.ceil
import kotlin.math.max

class ColorScreen(game: ClubGame) : BaseScreen(game) {
    private var currentStage = 1
    private var score = 0
    private var combo = 0
    private var timeLeft = 0f
    private var gameState = GameState.PLAYING
    private var mistakeInStage = false
    private val STAGE_TIME = 10f
    
    private var lastTouchMs = 0L
    private val MIN_TOUCH_INTERVAL_MS = 120L
    
    private lateinit var tiles: Array<Array<ColorTile>>
    private lateinit var correctTile: Vector2
    private lateinit var rng: Rng
    
    private val shapeRenderer = ShapeRenderer()
    private val baseColor = Color(0.3f, 0.5f, 0.8f, 1f)
    
    private var stageLabel: Label? = null
    private var scoreLabel: Label? = null
    private var timeLabel: Label? = null
    private var comboLabel: Label? = null
    
    enum class GameState {
        PLAYING, GAME_OVER
    }
    
    data class ColorTile(
        val rect: Rectangle,
        val color: Color,
        val isCorrect: Boolean = false
    )
    
    override fun setupUI() {
        rng = Rng()
        timeLeft = STAGE_TIME
        
        setupHUD()
        startNewStage()
    }
    
    private fun setupHUD() {
        val hudTable = Table()
        hudTable.setFillParent(true)
        hudTable.top().left()
        hudTable.pad(20f)
        
        stageLabel = UIComponents.createTitleLabel("스테이지: $currentStage", game.font).apply { setAlignment(Align.left) }
        scoreLabel = UIComponents.createSubtitleLabel("점수: $score", game.font).apply { setAlignment(Align.left) }
        timeLabel = UIComponents.createSubtitleLabel("시간: ${ceil(timeLeft)}초", game.font).apply { setAlignment(Align.left) }
        comboLabel = UIComponents.createSubtitleLabel("콤보: $combo", game.font).apply { setAlignment(Align.left) }
        
        hudTable.add(stageLabel).left().padBottom(10f).row()
        hudTable.add(scoreLabel).left().padBottom(5f).row()
        hudTable.add(comboLabel).left()
        
        stage.addActor(hudTable)
    }
    
    private fun startNewStage() {
        gameState = GameState.PLAYING
        timeLeft = STAGE_TIME
        mistakeInStage = false
        
        createTiles()
        updateHUD()
    }
    
    private fun createTiles() {
        val gridSize = gridSizeFor(currentStage)
        tiles = Array(gridSize) { Array(gridSize) { ColorTile(Rectangle(), Color()) } }
        
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val tileSize = (screenWidth * 0.6f / gridSize).coerceAtMost(screenHeight * 0.6f / gridSize)
        val startX = (screenWidth - tileSize * gridSize) / 2
        val startY = (screenHeight - tileSize * gridSize) / 2
        
        val correctX = rng.nextInt(gridSize)
        val correctY = rng.nextInt(gridSize)
        correctTile = Vector2(correctX.toFloat(), correctY.toFloat())
        
        baseColor.set(
            MathUtils.random(0.2f, 0.8f),
            MathUtils.random(0.2f, 0.8f),
            MathUtils.random(0.2f, 0.8f),
            1f
        )

        val delta = colorDeltaFor(currentStage)
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val rect = Rectangle(
                    startX + x * tileSize,
                    startY + y * tileSize,
                    tileSize * 0.9f,
                    tileSize * 0.9f
                )
                
                val isCorrect = (x == correctX && y == correctY)
                val color = if (isCorrect) {
                    if (game.settings.colorBlindMode) {
                        Color(baseColor).apply {
                            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
                            val change = (delta * 1.5f).coerceAtMost(0.3f)
                            if (luminance > 0.5f) {
                                r = (r - change).coerceIn(0f, 1f)
                                g = (g - change).coerceIn(0f, 1f)
                                b = (b - change).coerceIn(0f, 1f)
                            } else {
                                r = (r + change).coerceIn(0f, 1f)
                                g = (g + change).coerceIn(0f, 1f)
                                b = (b + change).coerceIn(0f, 1f)
                            }
                        }
                    } else {
                        Color(baseColor).apply {
                            r = (r + delta).coerceIn(0f, 1f)
                            g = (g + delta).coerceIn(0f, 1f)
                            b = (b + delta).coerceIn(0f, 1f)
                        }
                    }
                } else Color(baseColor)
                
                tiles[y][x] = ColorTile(rect, color, isCorrect)
            }
        }
    }
    
    private fun gridSizeFor(stage: Int): Int {
        var acc = 0
        for (size in 2..8) {
            val span = 3 * (size - 1)
            acc += span
            if (stage <= acc) return size
        }
        return 8
    }

    private fun colorDeltaFor(stage: Int): Float {
        val tier = (stage - 1) / 10
        val start = 0.18f
        val step = 0.02f
        val minDelta = 0.04f
        return max(minDelta, start - step * tier)
    }

    private fun updateHUD() {
        stageLabel?.setText("스테이지: $currentStage")
        scoreLabel?.setText("점수: $score")
        timeLabel?.setText("시간: ${ceil(timeLeft)}초")
        comboLabel?.setText("콤보: $combo")
    }
    
    override fun update(dt: Float) {
        when (gameState) {
            GameState.PLAYING -> {
                timeLeft -= dt
                if (timeLeft <= 0) {
                    gameOver()
                }
                
                updateHUD()
                handleInput()
            }
            GameState.GAME_OVER -> { }
        }
    }
    
    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val nowMs = TimeUtils.millis()
            if (nowMs - lastTouchMs < MIN_TOUCH_INTERVAL_MS) return
            val touchX = Gdx.input.x.toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()
            
            for (y in tiles.indices) {
                for (x in tiles[y].indices) {
                    val tile = tiles[y][x]
                    if (tile.rect.contains(touchX, touchY)) {
                        lastTouchMs = nowMs
                        handleTileClick(x, y)
                        return
                    }
                }
            }
        }
    }
    
    private fun handleTileClick(x: Int, y: Int) {
        val tile = tiles[y][x]
        
        if (tile.isCorrect) {
            val newCombo = if (!mistakeInStage) combo + 1 else 0
            val bonus = if (newCombo > 0) newCombo / 5 else 0
            val gained = 1 + bonus
            score += gained
            combo = newCombo

            Gdx.app.log("ColorGame", "Correct! +$gained (base=1, bonus=$bonus, combo=$combo, stage=$currentStage)")

            currentStage++
            startNewStage()
        } else {
            timeLeft = max(0f, timeLeft - 2f)
            combo = 0
            mistakeInStage = true
            Gdx.app.log("ColorGame", "Wrong! Combo reset")
            updateHUD()
        }
    }
    
    private fun gameOver() {
        gameState = GameState.GAME_OVER
        Gdx.app.log("ColorGame", "Game Over! Final Score: $score")
        game.router.goToColorGameOver(score)
    }
    
    override fun draw(alpha: Float) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        shapeRenderer.projectionMatrix = game.spriteBatch.projectionMatrix
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        for (row in tiles) {
            for (tile in row) {
                shapeRenderer.setColor(tile.color)
                shapeRenderer.rect(tile.rect.x, tile.rect.y, tile.rect.width, tile.rect.height)
            }
        }

        val barWidth = Gdx.graphics.width * 0.6f
        val barHeight = 16f
        val barX = (Gdx.graphics.width - barWidth) / 2f
        val barY = Gdx.graphics.height - 40f
        val ratio = (timeLeft / STAGE_TIME).coerceIn(0f, 1f)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.35f))
        shapeRenderer.rect(barX, barY, barWidth, barHeight)
        shapeRenderer.setColor(Color(0.2f, 0.8f, 0.3f, 1f))
        shapeRenderer.rect(barX, barY, barWidth * ratio, barHeight)
        
        shapeRenderer.end()

        val timeText = "${ceil(timeLeft).toInt()}초"
        val layout = GlyphLayout(game.font, timeText)
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
