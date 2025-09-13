package com.club.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable

class AssetRegistry : Disposable {
    val manager = AssetManager()
    lateinit var koreanFont: BitmapFont
        private set
    lateinit var titleFont: BitmapFont
        private set
    private var fontGenerator: FreeTypeFontGenerator? = null
    private var boldFontGenerator: FreeTypeFontGenerator? = null

    fun preload() {
        try {
            fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansKR-Regular.ttf"))
            val parameter = FreeTypeFontParameter().apply {
                size = 18
                incremental = true
                kerning = true
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
            }
            koreanFont = fontGenerator!!.generateFont(parameter)

            val boldHandle = Gdx.files.internal("fonts/NotoSansKR-Bold.ttf")
            if (boldHandle.exists()) {
                boldFontGenerator = FreeTypeFontGenerator(boldHandle)
                val titleParam = FreeTypeFontParameter().apply {
                    size = 42
                    incremental = true 
                    kerning = true
                    minFilter = Texture.TextureFilter.Linear
                    magFilter = Texture.TextureFilter.Linear
                }
                titleFont = boldFontGenerator!!.generateFont(titleParam)
            } else {
                val titleParam = FreeTypeFontParameter().apply {
                    size = 42
                    incremental = true
                    kerning = true
                    minFilter = Texture.TextureFilter.Linear
                    magFilter = Texture.TextureFilter.Linear
                }
                titleFont = fontGenerator!!.generateFont(titleParam)
            }
            Gdx.app.log("AssetRegistry", "Korean font loaded (incremental) successfully")
        } catch (e: Exception) {
            Gdx.app.error("AssetRegistry", "Failed to load Korean font, using default", e)
            koreanFont = BitmapFont()
            titleFont = koreanFont
        }
    }

    fun finishLoading() {
        manager.finishLoading()
    }

    override fun dispose() {
        if (this::koreanFont.isInitialized) koreanFont.dispose()
        if (this::titleFont.isInitialized && titleFont !== koreanFont) titleFont.dispose()
        fontGenerator?.dispose()
        boldFontGenerator?.dispose()
        manager.dispose()
    }
}

