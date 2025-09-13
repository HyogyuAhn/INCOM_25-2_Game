package com.club

import com.club.screens.ColorScreen
import com.club.screens.MenuScreen
import com.club.screens.ReflexScreen
import com.club.screens.ShooterScreen
import com.club.screens.SettingsScreen
import com.club.screens.ColorGameOverScreen
import com.club.screens.ReflexGameOverScreen

enum class ScreenId { MENU, COLOR, MEMORY, SHOOTER, SETTINGS }

class ScreenRouter(private val game: ClubGame) {
    fun goTo(id: ScreenId) {
        val newScreen = when (id) {
            ScreenId.MENU -> MenuScreen(game)
            ScreenId.COLOR -> ColorScreen(game)
            ScreenId.MEMORY -> ReflexScreen(game)
            ScreenId.SHOOTER -> ShooterScreen(game)
            ScreenId.SETTINGS -> SettingsScreen(game)
        }
        game.setScreen(newScreen)
    }

    fun goToColorGameOver(score: Int) {
        game.setScreen(ColorGameOverScreen(game, score))
    }

    fun goToReflexGameOver(score: Int) {
        game.setScreen(ReflexGameOverScreen(game, score))
    }
}
