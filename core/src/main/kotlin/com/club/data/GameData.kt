package com.club.data

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

data class ColorLevel(
    val stage: Int = 0,
    val grid: Int = 0,
    val delta: Float = 0f,
    val timeLimit: Float = 0f
)

data class MemoryLevel(
    val stage: Int = 0,
    val rows: Int = 0,
    val cols: Int = 0,
    val pairs: Int = 0,
    val attempts: Int = 0,
    val hintCount: Int = 0,
    val timeLimit: Float = 0f
)

data class ShooterConfig(
    val player: PlayerConfig = PlayerConfig(),
    val enemies: List<EnemyConfig> = emptyList(),
    val bosses: List<BossConfig> = emptyList(),
    val stages: List<StageConfig> = emptyList()
)

data class PlayerConfig(
    val moveSpeed: Float = 0f,
    val dashCooldown: Float = 0f,
    val dashTime: Float = 0f,
    val iframes: Float = 0f,
    val life: Int = 0,
    val fireCooldown: Float = 0.2f,
    val bulletSpeed: Float = 400f,
    val bulletSize: Float = 4f,
    val bulletDamage: Int = 1
)

data class EnemyConfig(
    val id: String = "",
    val size: Float = 0f,
    val speed: Float = 0f,
    val hp: Int = 0,
    val pattern: String = ""
)

data class BossConfig(
    val id: String = "",
    val hpScale: Float = 0f,
    val phases: List<String> = emptyList(),
    val phaseThresholds: List<Float> = emptyList()
)

data class StageConfig(
    val stage: Int = 0,
    val spawnInterval: Float = 0f,
    val weights: HashMap<String, Float> = HashMap(),
    val hpScale: Float = 0f,
    val speedScale: Float = 0f,
    val bossThreshold: Int = 0
)

object GameDataLoader {
    private val json = Json()
    
    fun loadColorLevels(): List<ColorLevel> {
        val file = Gdx.files.internal("data/game1_levels.json")
        return json.fromJson(Array<ColorLevel>::class.java, file).toList()
    }
    
    fun loadMemoryLevels(): List<MemoryLevel> {
        val file = Gdx.files.internal("data/game2_levels.json")
        return json.fromJson(Array<MemoryLevel>::class.java, file).toList()
    }
    
    fun loadShooterConfig(): ShooterConfig {
        val file = Gdx.files.internal("data/game3_config.json")
        return json.fromJson(ShooterConfig::class.java, file)
    }
}
