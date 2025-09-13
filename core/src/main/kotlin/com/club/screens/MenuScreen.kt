package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.club.ClubGame
import com.club.ScreenId
import com.club.ui.UIComponents
import com.club.ui.UITheme

class MenuScreen(game: ClubGame) : BaseScreen(game) {
    private val bgRenderer = ShapeRenderer()
    private lateinit var root: Table
    private lateinit var panel: Table
    private lateinit var gamesRow: Table
    private lateinit var actionsRow: Table
    private var settingsOverlay: Table? = null
    
    override fun setupUI() {
        root = Table().apply {
            setFillParent(true)
            center()
        }
        
        panel = UIComponents.createPanel(32f)
        val titleStack = UIComponents.createTitleWithShadow(
            text = "INCOM 25-2",
            font = game.assets.titleFont,
            scale = 2.4f,
            color = com.club.ui.UITheme.accent,
            shadowColor = Color(0f, 0f, 0f, 0.5f),
            dx = 2f,
            dy = 2f
        )
        val subtitleLabel = UIComponents.createSubtitleLabel("미니게임 모음", game.font).apply {
            setFontScale(1.0f)
        }

        gamesRow = Table()
        actionsRow = Table()

        val colorButton = UIComponents.createModernButton("색상 찾기", game.font) {
            game.router.goTo(ScreenId.COLOR)
        }
        val memoryButton = UIComponents.createModernButton("순발력 게임", game.font) {
            game.router.goTo(ScreenId.MEMORY)
        }
        val shooterButton = UIComponents.createModernButton("슈팅 게임", game.font) {
            game.router.goTo(ScreenId.SHOOTER)
        }
        val settingsButton = UIComponents.createModernButton("설정", game.font) { openSettingsOverlay() }
        val exitButton = UIComponents.createModernButton("게임 종료", game.font) {
            Gdx.app.exit()
        }
        gamesRow.add(colorButton).width(240f).height(56f).padRight(12f)
        gamesRow.add(memoryButton).width(240f).height(56f).padRight(12f)
        gamesRow.add(shooterButton).width(240f).height(56f)
        actionsRow.add(settingsButton).width(180f).height(52f).padRight(12f)
        actionsRow.add(exitButton).width(180f).height(52f)

        panel.add(titleStack).padBottom(12f).row()
        panel.add(subtitleLabel).padBottom(20f).row()
        panel.add(gamesRow).padBottom(16f).row()
        panel.add(actionsRow)

        root.add(panel)
        stage.addActor(root)

        panel.color.a = 0f
        panel.y -= 20f
        panel.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeIn(0.35f),
                Actions.moveBy(0f, 20f, 0.45f)
            )
        ))
        titleStack.color.a = 0f
        titleStack.addAction(Actions.fadeIn(0.5f))
    }

    private fun openSettingsOverlay() {
        if (settingsOverlay != null) return
        gamesRow.isVisible = false
        actionsRow.isVisible = false

        val overlay = Table().apply {
            setFillParent(true)
            background = UITheme.dim
        }

        val modal = Table().apply {
            background = UITheme.panelBg
            pad(24f)
        }

        val title = UIComponents.createTitleWithShadow(
            text = "설정",
            font = game.assets.titleFont,
            scale = 1.8f,
            color = UITheme.accent
        )

        val header = Table().apply {
            add(title).expandX().left()
            val closeLabel = UIComponents.createSubtitleLabel("닫기", game.font).apply { color = UITheme.accent }
            closeLabel.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { closeSettingsOverlay() }
            })
            val closeContainer = Container(closeLabel).pad(8f)
            add(closeContainer).right()
        }

        val body = VerticalGroup().apply {
            space(14f)
            align(Align.center)
        }
        lateinit var debugBtn: TextButton
        fun refreshDebug() {
            val state = if (game.debugOverlay.enabled) "켜짐" else "꺼짐"
            debugBtn.setText("FPS 표시: $state")
        }
        debugBtn = UIComponents.createModernButton("", game.font) {
            game.debugOverlay.enabled = !game.debugOverlay.enabled
            Gdx.app.log("DebugHUD", "enabled=${'$'}{game.debugOverlay.enabled}")
            refreshDebug()
        }
        refreshDebug()

        lateinit var cbBtn: TextButton
        fun refreshCb() {
            val state = if (game.settings.colorBlindMode) "켜짐" else "꺼짐"
            cbBtn.setText("색약 보정: $state")
        }
        cbBtn = UIComponents.createModernButton("", game.font) {
            game.updateSettings { it.copy(colorBlindMode = !it.colorBlindMode) }
            refreshCb()
        }
        refreshCb()

        listOf(debugBtn, cbBtn).forEach { it.setSize(300f, 48f) }
        body.addActor(debugBtn)
        body.addActor(cbBtn)

        modal.add(header).growX().padBottom(16f).row()
        modal.add(body).center()

        overlay.add(modal).width(480f).center()

        overlay.color.a = 0f
        overlay.addAction(Actions.fadeIn(0.25f))
        settingsOverlay = overlay
        stage.addActor(overlay)
    }

    private fun closeSettingsOverlay() {
        val overlay = settingsOverlay ?: return
        overlay.addAction(Actions.sequence(
            Actions.fadeOut(0.2f),
            Actions.run {
                overlay.remove()
                settingsOverlay = null
                gamesRow.isVisible = true
                actionsRow.isVisible = true
            }
        ))
    }

    override fun update(dt: Float) { }

    override fun draw(alpha: Float) {
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val w = stage.viewport.worldWidth
        val h = stage.viewport.worldHeight
        bgRenderer.projectionMatrix = stage.camera.combined
        bgRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val top = Color(0.10f, 0.14f, 0.20f, 1f)
        val bottom = Color(0.04f, 0.05f, 0.09f, 1f)
        bgRenderer.rect(0f, 0f, w, h, bottom, bottom, top, top)
        bgRenderer.end()
    }

    override fun wantsBlurOverlay(): Boolean = settingsOverlay != null

    override fun beforeStageDrawForBlur() {
        settingsOverlay?.isVisible = false
    }

    override fun drawOverlayAfterBlur() {
        val overlay = settingsOverlay ?: return
        val prevRoot = root.isVisible
        root.isVisible = false
        overlay.isVisible = true
        stage.draw()
        root.isVisible = prevRoot
        overlay.isVisible = true
    }

    override fun dispose() {
        super.dispose()
        bgRenderer.dispose()
    }
}

