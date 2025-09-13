package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.club.ClubGame

abstract class BaseScreen(protected val game: ClubGame) : ScreenAdapter() {
    private val updateDt: Float = 1f / 60f
    private var accumulator: Float = 0f
    private var paused: Boolean = false
    
    protected lateinit var stage: Stage
        private set

    private var sceneFbo: FrameBuffer? = null
    private var smallFbo: FrameBuffer? = null
    private val fboRegionScene = TextureRegion()
    private val fboRegionSmall = TextureRegion()

    override fun show() {
        stage = Stage(FitViewport(1280f, 720f))
        Gdx.input.inputProcessor = stage
        setupUI()
    }

    override fun render(delta: Float) {
        handleCommonInput()

        val clamped = delta.coerceIn(0f, 0.25f)
        accumulator += clamped

        while (accumulator >= updateDt) {
            if (!paused) update(updateDt)
            accumulator -= updateDt
        }
        val alpha = (accumulator / updateDt).coerceIn(0f, 1f)

        stage.act(delta)

        if (wantsBlurOverlay()) {
            ensureFbos()
            sceneFbo!!.begin()
            Gdx.gl.glViewport(0, 0, sceneFbo!!.width, sceneFbo!!.height)
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            
            
            draw(alpha)
            beforeStageDrawForBlur()
            stage.draw()
            afterStageDrawForBlur()
            sceneFbo!!.end()

            val batch = game.spriteBatch
            val sceneTex = sceneFbo!!.colorBufferTexture.apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
            fboRegionScene.setRegion(sceneTex)
            fboRegionScene.flip(false, true)

            smallFbo!!.begin()
            Gdx.gl.glViewport(0, 0, smallFbo!!.width, smallFbo!!.height)
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            batch.projectionMatrix.setToOrtho2D(0f, 0f, smallFbo!!.width.toFloat(), smallFbo!!.height.toFloat())
            batch.begin()
            batch.draw(fboRegionScene, 0f, 0f, smallFbo!!.width.toFloat(), smallFbo!!.height.toFloat())
            batch.end()
            smallFbo!!.end()

            val w = Gdx.graphics.width
            val h = Gdx.graphics.height
            val smallTex = smallFbo!!.colorBufferTexture.apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
            fboRegionSmall.setRegion(smallTex)
            fboRegionSmall.flip(false, true)
            Gdx.gl.glViewport(0, 0, w, h)
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            batch.projectionMatrix.setToOrtho2D(0f, 0f, w.toFloat(), h.toFloat())
            batch.begin()
            
            val vx = stage.viewport.screenX.toFloat()
            val vy = stage.viewport.screenY.toFloat()
            val vw = stage.viewport.screenWidth.toFloat()
            val vh = stage.viewport.screenHeight.toFloat()
            batch.draw(fboRegionSmall, vx, vy, vw, vh)
            batch.end()

            
            stage.viewport.apply(true)
            drawOverlayAfterBlur()
        } else {
            
            stage.viewport.apply(true)
            draw(alpha)
            stage.draw()
        }

        if (game.debugOverlay.enabled) {
            game.spriteBatch.begin()
            game.debugOverlay.render(game.spriteBatch)
            game.spriteBatch.end()
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        disposeFbos()
    }

    override fun dispose() {
        stage.dispose()
        disposeFbos()
    }

    protected open fun handleCommonInput() {
    }

    protected abstract fun setupUI()
    protected abstract fun update(dt: Float)
    protected abstract fun draw(alpha: Float)

    protected fun setPaused(value: Boolean) { paused = value }
    protected fun isPaused(): Boolean = paused

    protected open fun wantsBlurOverlay(): Boolean = false
    protected open fun beforeStageDrawForBlur() {}
    protected open fun afterStageDrawForBlur() {}
    protected open fun drawOverlayAfterBlur() {}

    private fun ensureFbos() {
        val w = Gdx.graphics.width
        val h = Gdx.graphics.height
        if (sceneFbo == null || sceneFbo!!.width != w || sceneFbo!!.height != h) {
            sceneFbo?.dispose()
            sceneFbo = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
        }
        val sw = (w / 4).coerceAtLeast(1)
        val sh = (h / 4).coerceAtLeast(1)
        if (smallFbo == null || smallFbo!!.width != sw || smallFbo!!.height != sh) {
            smallFbo?.dispose()
            smallFbo = FrameBuffer(Pixmap.Format.RGBA8888, sw, sh, false)
        }
    }

    private fun disposeFbos() {
        sceneFbo?.dispose(); sceneFbo = null
        smallFbo?.dispose(); smallFbo = null
    }
}

