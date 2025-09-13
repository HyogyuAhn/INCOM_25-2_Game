package com.club.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class DebugOverlay(private val font: BitmapFont) {
    var enabled: Boolean = false

    fun render(batch: SpriteBatch) {
        val fps = Gdx.graphics.framesPerSecond
        val sb = StringBuilder()
            .append("FPS: ").append(fps)
        val prevColor = Color(font.color)
        try {
            font.color = if (fps >= 60) Color.GREEN else Color.ORANGE
            font.draw(batch, sb.toString(), 16f, 20f)
        } finally {
            font.color = prevColor
        }
    }
}

