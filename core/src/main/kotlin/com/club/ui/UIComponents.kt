package com.club.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.math.Interpolation

object UIComponents {
    private val externalSkin: Skin? by lazy {
        try {
            val file = Gdx.files.internal("skins/shade/shade-ui.json")
            if (file.exists()) Skin(file) else null
        } catch (_: Throwable) { null }
    }
    fun createModernButton(
        text: String,
        font: BitmapFont,
        onClick: () -> Unit
    ): TextButton {
        val skin = Skin()
        val buttonStyle = TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = UITheme.textPrimary
            overFontColor = UITheme.textPrimary
            downFontColor = UITheme.textSecondary
            up = UITheme.btnUp
            over = UITheme.btnOver
            down = UITheme.btnDown
        }
        skin.add("default", buttonStyle)
        val button = TextButton(text, skin)
        button.label.setAlignment(Align.center)
        button.pad(14f, 24f, 14f, 24f)
        button.setTransform(true)
        button.setOrigin(button.width / 2f, button.height / 2f)
        button.addListener(object : ClickListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                button.setOrigin(button.width / 2f, button.height / 2f)
                button.clearActions()
                button.addAction(Actions.scaleTo(1.03f, 1.03f, 0.08f, Interpolation.sine))
            }
            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                button.setOrigin(button.width / 2f, button.height / 2f)
                button.clearActions()
                button.addAction(Actions.scaleTo(1f, 1f, 0.08f, Interpolation.sine))
            }
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, buttonCode: Int): Boolean {
                button.setOrigin(button.width / 2f, button.height / 2f)
                button.clearActions()
                button.addAction(Actions.scaleTo(0.98f, 0.98f, 0.05f, Interpolation.sine))
                return super.touchDown(event, x, y, pointer, buttonCode)
            }
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, buttonCode: Int) {
                super.touchUp(event, x, y, pointer, buttonCode)
                button.setOrigin(button.width / 2f, button.height / 2f)
                button.clearActions()
                button.addAction(Actions.scaleTo(1.03f, 1.03f, 0.08f, Interpolation.sine))
            }
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onClick()
            }
        })
        
        return button
    }
    
    fun createTitleLabel(text: String, font: BitmapFont): Label {
        val labelStyle = Label.LabelStyle(font, UITheme.textPrimary)
        val label = Label(text, Skin().apply { add("default", labelStyle) })
        label.setAlignment(Align.center)
        return label
    }
    
    fun createSubtitleLabel(text: String, font: BitmapFont): Label {
        val labelStyle = Label.LabelStyle(font, UITheme.textSecondary)
        val label = Label(text, Skin().apply { add("default", labelStyle) })
        label.setAlignment(Align.center)
        return label
    }

    fun createTitleWithShadow(
        text: String,
        font: BitmapFont,
        scale: Float = 1.0f,
        color: com.badlogic.gdx.graphics.Color = UITheme.textPrimary,
        shadowColor: com.badlogic.gdx.graphics.Color = com.badlogic.gdx.graphics.Color(0f, 0f, 0f, 0.45f),
        dx: Float = 2f,
        dy: Float = 2f
    ): Stack {
        val top = createTitleLabel(text, font).apply {
            setFontScale(scale)
            this.color = color
        }
        val shadow = createTitleLabel(text, font).apply {
            setFontScale(scale)
            this.color = shadowColor
        }
        val shadowContainer = Container(shadow).padLeft(dx).padTop(dy)
        val stack = Stack()
        stack.addActor(shadowContainer)
        stack.addActor(top)
        return stack
    }

    fun createPanel(padding: Float = 24f): Table {
        val t = Table()
        t.background = UITheme.panelBg
        t.pad(padding)
        return t
    }
}

