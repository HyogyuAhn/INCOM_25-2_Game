package com.club.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Disposable

object UITheme : Disposable {
    val accent: Color = Color(0.38f, 0.78f, 1.0f, 1.0f)
    val textPrimary: Color = Color.WHITE
    val textSecondary: Color = Color(0.8f, 0.86f, 0.92f, 1f)

    private val ownedTextures = mutableListOf<Texture>()

    
    val btnUp: Drawable = ninePatchSolidWithBorder(
        fill = Color(0.46f, 0.56f, 0.70f, 1f),
        border = Color(1f, 1f, 1f, 0.12f),
        borderWidth = 1,
        centerSize = 4
    )
    val btnOver: Drawable = ninePatchSolidWithBorder(
        fill = Color(0.54f, 0.65f, 0.80f, 1f),
        border = Color(1f, 1f, 1f, 0.16f),
        borderWidth = 1,
        centerSize = 4
    )
    val btnDown: Drawable = ninePatchSolidWithBorder(
        fill = Color(0.38f, 0.46f, 0.60f, 1f),  
        border = Color(1f, 1f, 1f, 0.10f),
        borderWidth = 1,
        centerSize = 4
    )
    val panelBg: Drawable = ninePatchSolidWithBorder(
        fill = Color(0f, 0f, 0f, 0.62f),
        border = Color(1f, 1f, 1f, 0.22f),
        borderWidth = 1,
        centerSize = 6
    )
    val panelPlain: Drawable = solid(Color(0f, 0f, 0f, 0.36f), 2, 2)
    val dim: Drawable = solid(Color(0f, 0f, 0f, 0.35f), 2, 2)

    private fun solid(color: Color, width: Int = 4, height: Int = 4): Drawable {
        val pm = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fill()
        val tex = Texture(pm)
        pm.dispose()
        ownedTextures += tex
        return TextureRegionDrawable(TextureRegion(tex))
    }

    private fun ninePatchSolidWithBorder(
        fill: Color,
        border: Color,
        borderWidth: Int = 1,
        centerSize: Int = 3
    ): Drawable {
        val bw = borderWidth.coerceAtLeast(1)
        val cs = centerSize.coerceAtLeast(1)
        val w = bw * 2 + cs
        val h = bw * 2 + cs
        val pm = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pm.setColor(border)
        pm.fill()
        pm.setColor(fill)
        pm.fillRectangle(bw, bw, cs, cs)
        val tex = Texture(pm)
        pm.dispose()
        ownedTextures += tex
        val np = NinePatch(tex, bw, bw, bw, bw)
        return NinePatchDrawable(np)
    }

    private fun gradient(top: Color, bottom: Color, width: Int = 8, height: Int = 64): Drawable {
        val pm = Pixmap(width, height, Pixmap.Format.RGBA8888)
        val h = height.coerceAtLeast(1)
        for (y in 0 until h) {
            val t = y.toFloat() / (h - 1).coerceAtLeast(1)
            val r = bottom.r + (top.r - bottom.r) * t
            val g = bottom.g + (top.g - bottom.g) * t
            val b = bottom.b + (top.b - bottom.b) * t
            val a = bottom.a + (top.a - bottom.a) * t
            pm.setColor(r, g, b, a)
            pm.drawLine(0, y, width - 1, y)
        }
        val tex = Texture(pm)
        pm.dispose()
        ownedTextures += tex
        return TextureRegionDrawable(TextureRegion(tex))
    }

    private fun gradientWithBorder(
        top: Color,
        bottom: Color,
        border: Color,
        width: Int = 8,
        height: Int = 64,
        borderWidth: Int = 1
    ): Drawable {
        val pm = Pixmap(width, height, Pixmap.Format.RGBA8888)
        val h = height.coerceAtLeast(1)
        for (y in 0 until h) {
            val t = y.toFloat() / (h - 1).coerceAtLeast(1)
            val r = bottom.r + (top.r - bottom.r) * t
            val g = bottom.g + (top.g - bottom.g) * t
            val b = bottom.b + (top.b - bottom.b) * t
            val a = bottom.a + (top.a - bottom.a) * t
            pm.setColor(r, g, b, a)
            pm.drawLine(0, y, width - 1, y)
        }
        pm.setColor(border)
        for (i in 0 until borderWidth) {
            pm.drawRectangle(i, i, width - 1 - i * 2, height - 1 - i * 2)
        }
        val tex = Texture(pm)
        pm.dispose()
        ownedTextures += tex
        return TextureRegionDrawable(TextureRegion(tex))
    }

    override fun dispose() {
        ownedTextures.forEach { it.dispose() }
        ownedTextures.clear()
    }
}
