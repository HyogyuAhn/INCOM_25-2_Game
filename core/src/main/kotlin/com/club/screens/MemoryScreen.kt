package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.club.ClubGame
import com.club.data.GameDataLoader
import com.club.data.MemoryLevel
import com.club.ui.UIComponents
import com.club.utils.Rng
import kotlin.math.ceil

class MemoryScreen(game: ClubGame) : BaseScreen(game) {
    private var currentStage = 1
    private var score = 0
    private var combo = 0
    private var timeLeft = 0f
    private var attempts = 0
    private var gameState = GameState.PLAYING
    
    private lateinit var levels: List<MemoryLevel>
    private lateinit var currentLevel: MemoryLevel
    private lateinit var cards: Array<Array<MemoryCard>>
    private lateinit var rng: Rng
    
    private val shapeRenderer = ShapeRenderer()
    private var firstPick: Pair<Int, Int>? = null
    private var secondPick: Pair<Int, Int>? = null
    private var resolveTimer = 0f
    private var hintTimer = 0f
    private var showHint = false
    
    private var stageLabel: Label? = null
    private var scoreLabel: Label? = null
    private var timeLabel: Label? = null
    private var attemptsLabel: Label? = null
    private var comboLabel: Label? = null
    private var messageLabel: Label? = null
    private var messageTable: Table? = null
    
    enum class GameState {
        PLAYING, GAME_OVER, VICTORY
    }
    
    enum class CardState {
        HIDDEN, REVEALED, MATCHED
    }
    
    data class MemoryCard(
        val rect: Rectangle,
        val pairId: Int,
        var state: CardState = CardState.HIDDEN,
        val color: Color = Color(0.2f, 0.3f, 0.5f, 1f)
    )
    
    override fun setupUI() {
        levels = GameDataLoader.loadMemoryLevels()
        rng = Rng()
        
        setupHUD()
        startNewStage()
    }
    
    private fun setupHUD() {
        val hudTable = Table()
        hudTable.setFillParent(true)
        hudTable.top().left()
        hudTable.pad(20f)
        
        stageLabel = UIComponents.createTitleLabel("스테이지: $currentStage", game.font)
        scoreLabel = UIComponents.createSubtitleLabel("점수: $score", game.font)
        timeLabel = UIComponents.createSubtitleLabel("시간: ${ceil(timeLeft)}", game.font)
        attemptsLabel = UIComponents.createSubtitleLabel("기회: $attempts", game.font)
        comboLabel = UIComponents.createSubtitleLabel("콤보: $combo", game.font)
        
        hudTable.add(stageLabel).padBottom(10f).row()
        hudTable.add(scoreLabel).padBottom(5f).row()
        hudTable.add(timeLabel).padBottom(5f).row()
        hudTable.add(attemptsLabel).padBottom(5f).row()
        hudTable.add(comboLabel)
        
        stage.addActor(hudTable)

        
        messageLabel = UIComponents.createTitleLabel("", game.font).apply {
            setAlignment(Align.center)
            setFontScale(2f)
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
    
    private fun startNewStage() {
        messageLabel?.isVisible = false
        if (currentStage > levels.size) {
            gameState = GameState.VICTORY
            messageLabel?.setText("승리! 최종 점수: $score")
            messageLabel?.isVisible = true
            return
        }
        
        currentLevel = levels[currentStage - 1]
        timeLeft = currentLevel.timeLimit
        attempts = currentLevel.attempts
        gameState = GameState.PLAYING
        firstPick = null
        secondPick = null
        resolveTimer = 0f
        hintTimer = 0f
        showHint = false
        
        createCards()
        updateHUD()
    }
    
    private fun createCards() {
        val rows = currentLevel.rows
        val cols = currentLevel.cols
        val pairs = currentLevel.pairs
        
        cards = Array(rows) { Array(cols) { MemoryCard(Rectangle(), 0) } }
        
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val cardWidth = (screenWidth * 0.8f / cols).coerceAtMost(100f)
        val cardHeight = (screenHeight * 0.6f / rows).coerceAtMost(80f)
        val startX = (screenWidth - cardWidth * cols) / 2
        val startY = (screenHeight - cardHeight * rows) / 2 + 100f
        
        
        val pairIds = mutableListOf<Int>()
        repeat(pairs) { pairId ->
            pairIds.add(pairId)
            pairIds.add(pairId)
        }
        pairIds.shuffle()
        
        var pairIndex = 0
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val rect = Rectangle(
                    startX + x * cardWidth,
                    startY + y * cardHeight,
                    cardWidth * 0.9f,
                    cardHeight * 0.9f
                )
                
                val pairId = if (pairIndex < pairIds.size) pairIds[pairIndex++] else 0
                cards[y][x] = MemoryCard(rect, pairId)
            }
        }
    }
    
    private fun updateHUD() {
        stageLabel?.setText("스테이지: $currentStage")
        scoreLabel?.setText("점수: $score")
        timeLabel?.setText("시간: ${ceil(timeLeft)}")
        attemptsLabel?.setText("기회: $attempts")
        comboLabel?.setText("콤보: $combo")
    }
    
    override fun update(dt: Float) {
        when (gameState) {
            GameState.PLAYING -> {
                timeLeft -= dt
                if (timeLeft <= 0) {
                    gameOver()
                }
                
                
                if (hintTimer > 0) {
                    hintTimer -= dt
                    if (hintTimer <= 0) {
                        showHint = false
                    }
                }
                
                
                if (resolveTimer > 0) {
                    resolveTimer -= dt
                    if (resolveTimer <= 0) {
                        resolvePicks()
                    }
                } else {
                    handleInput()
                }
                
                
                if (currentLevel.hintCount > 0 && timeLeft < currentLevel.timeLimit * 0.3f) {
                    showHint = true
                    hintTimer = 2f
                }
            }
            GameState.GAME_OVER, GameState.VICTORY -> {
                
            }
        }
    }
    
    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()
            
            for (y in cards.indices) {
                for (x in cards[y].indices) {
                    val card = cards[y][x]
                    if (card.rect.contains(touchX, touchY) && card.state == CardState.HIDDEN) {
                        handleCardClick(x, y)
                        return
                    }
                }
            }
        }
    }
    
    private fun handleCardClick(x: Int, y: Int) {
        val card = cards[y][x]
        card.state = CardState.REVEALED
        
        when {
            firstPick == null -> {
                firstPick = Pair(x, y)
            }
            secondPick == null -> {
                secondPick = Pair(x, y)
                resolveTimer = 1f 
            }
        }
    }
    
    private fun resolvePicks() {
        val first = firstPick
        val second = secondPick
        
        if (first != null && second != null) {
            val firstCard = cards[first.second][first.first]
            val secondCard = cards[second.second][second.first]
            
            if (firstCard.pairId == secondCard.pairId) {
                
                firstCard.state = CardState.MATCHED
                secondCard.state = CardState.MATCHED
                combo++
                
                val matchScore = 50 * currentStage * (1f + 0.05f * combo)
                score += matchScore.toInt()
                
                Gdx.app.log("MemoryGame", "Match! Score: ${matchScore.toInt()}, Combo: $combo")
                
                
                if (cards.all { row -> row.all { it.state == CardState.MATCHED } }) {
                    val timeBonus = ceil(timeLeft * 5).toInt()
                    score += timeBonus
                    currentStage++
                    startNewStage()
                }
            } else {
                
                firstCard.state = CardState.HIDDEN
                secondCard.state = CardState.HIDDEN
                combo = 0
                attempts--
                
                if (attempts <= 0) {
                    gameOver()
                }
                
                Gdx.app.log("MemoryGame", "Mismatch! Attempts left: $attempts")
            }
        }
        
        firstPick = null
        secondPick = null
        updateHUD()
    }
    
    private fun gameOver() {
        gameState = GameState.GAME_OVER
        messageLabel?.setText("게임 오버! 점수: $score")
        messageLabel?.isVisible = true
        Gdx.app.log("MemoryGame", "Game Over! Final Score: $score")
    }
    
    override fun draw(alpha: Float) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        
        shapeRenderer.projectionMatrix = game.spriteBatch.projectionMatrix
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        for (row in cards) {
            for (card in row) {
                when (card.state) {
                    CardState.HIDDEN -> {
                        shapeRenderer.setColor(Color(0.3f, 0.4f, 0.6f, 1f))
                    }
                    CardState.REVEALED -> {
                        val hue = (card.pairId * 137.5f) % 360f
                        val color = Color()
                        color.fromHsv(hue, 0.7f, 0.8f)
                        shapeRenderer.setColor(color)
                    }
                    CardState.MATCHED -> {
                        shapeRenderer.setColor(Color(0.2f, 0.8f, 0.2f, 1f))
                    }
                }
                shapeRenderer.rect(card.rect.x, card.rect.y, card.rect.width, card.rect.height)
            }
        }
        
        shapeRenderer.end()
    }
    
    override fun dispose() {
        super.dispose()
        shapeRenderer.dispose()
    }
}

