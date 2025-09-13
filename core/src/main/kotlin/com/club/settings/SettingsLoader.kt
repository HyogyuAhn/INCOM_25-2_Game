package com.club.settings

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter

object SettingsLoader {
    private const val SETTINGS_PATH = "data/settings.json"

    fun load(): Settings {
        val fh: FileHandle = Gdx.files.internal(SETTINGS_PATH)
        return if (fh.exists()) {
            try {
                val json = Json()
                json.setIgnoreUnknownFields(true)
                json.setOutputType(JsonWriter.OutputType.json)
                json.fromJson(Settings::class.java, fh)
            } catch (t: Throwable) {
                Gdx.app.error("Settings", "Failed to load settings, using defaults", t)
                Settings()
            }
        } else {
            Gdx.app.log("Settings", "No settings file, using defaults")
            Settings()
        }
    }
}

