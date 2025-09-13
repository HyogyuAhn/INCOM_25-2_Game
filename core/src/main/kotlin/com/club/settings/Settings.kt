package com.club.settings

data class Settings(
    val masterVolume: Float = 0.8f,
    val difficultyPreset: String = "normal",
    val flashIntensity: Float = 0.3f,
    val vibrationIntensity: Float = 0.3f,
    val colorBlindMode: Boolean = false,
)
