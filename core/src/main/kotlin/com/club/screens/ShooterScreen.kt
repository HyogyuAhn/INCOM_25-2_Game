package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.club.ClubGame

class ShooterScreen(game: ClubGame) : BaseScreen(game) {
    
    private val worldW = 1280f
    private val worldH = 720f

    
    private val shape = ShapeRenderer()
    private val layout = GlyphLayout()

    
    private val playerPos = Vector2(worldW * 0.5f, worldH * 0.15f)
    private val playerSpeed = 360f
    private val playerRadius = 18f
    private var playerHearts = 4
    private var playerMaxHearts = 6
    private var playerAtk = 1

    
    private var firing = false
    private var fireCooldown = 0f
    private val baseFireInterval = (1f / 4.2f)
    private var fireInterval = baseFireInterval

    private data class Bullet(
        val pos: Vector2 = Vector2(),
        val vel: Vector2 = Vector2(),
        var radius: Float = 8f,
        var dmg: Int = 1,
        var alive: Boolean = true,
        var pierce: Int = 0,
        val hit: MutableSet<Int> = mutableSetOf(),
        var fromDrone: Boolean = false
    )

    private val bullets = mutableListOf<Bullet>()

    
    private data class EnemyBullet(
        val pos: Vector2 = Vector2(),
        val vel: Vector2 = Vector2(),
        var radius: Float = 5f,
        var alive: Boolean = true,
        var homing: Boolean = false,
        var life: Float = 0f,
        var maxLife: Float = 0f,
        var type: Int = 0 
    )
    private val enemyBullets = mutableListOf<EnemyBullet>()

    
    private data class Enemy(
        val pos: Vector2 = Vector2(),
        var vel: Vector2 = Vector2(0f, -120f),
        var radius: Float = 18f,
        var hp: Int = 1,
        var alive: Boolean = true,
        var scoreValue: Int = 1,
        var pattern: Int = 0, 
        var time: Float = 0f,
        var baseX: Float = 0f,
        var amp: Float = 0f,
        var omega: Float = 0f,
        var shootTimer: Float = MathUtils.random(0.8f, 1.4f),
        var tripleShooter: Boolean = false,
        var healer: Boolean = false,
        var bomber: Boolean = false,
        var splitter: Boolean = false,
        var child: Boolean = false,
        var edgeTime: Float = 0f,
        var id: Int = 0
    )

    private val enemies = mutableListOf<Enemy>()
    private var nextEnemyId = 1
    private var spawnTimer = 0f
    private var spawnInterval = 0.9f

    
    private var iFrames = 0f
    private var blinkTimer = 0f
    private var gameOver = false
    private var paused = false

    
    private var score = 0
    private var stageNumber = 1
    private var stageKills = 0
    private var stageTarget = requiredKillsForStage(stageNumber)

    
    private var u1StraightCount = 0      
    private var u2DiagonalLevel = 0      
    private var u3BulletSize = 0         
    private var u4PierceLevel = 0        
    private var u5MoveSpeed = 0          
    private var u9AtkBonus = 0           
    private var u6MissileLevel = 0       
    private var u7DroneLevel = 0         
    private var u8LaserDroneLevel = 0    

    private var upgradeMsg: String? = null
    private var upgradeMsgTimer = 0f

    
    private var qCd = 0f
    private var wCd = 0f
    private var eCd = 0f
    private var rCd = 0f
    
    private var debugEnabled = false
    private var qActive = false
    private var qActiveTime = 0f
    private var qTickTimer = 0f
    private val qTickInterval = 1f / 18f
    private var wActive = false
    private var wTimeLeft = 0f
    private var enemyShootDisabledTimer = 0f

    
    private data class Missile(
        val pos: Vector2 = Vector2(),
        val vel: Vector2 = Vector2(),
        var radius: Float = 6f,
        var alive: Boolean = true,
        var angleDeg: Float = 90f, 
        var life: Float = 3f,      
        var hasRetargeted: Boolean = false,
        var targetId: Int = 0,
        var dmg: Int = 0
    )
    private val missiles = mutableListOf<Missile>()
    private var missileSpawnTimer = 0f

    
    private data class UpgradeKit(
        val pos: Vector2 = Vector2(),
        val vel: Vector2 = Vector2(),
        var radius: Float = 10f,
        var alive: Boolean = true,
        var life: Float = 14f
    )
    private val upgradeKits = mutableListOf<UpgradeKit>()
    
    
    private fun upgradeKitChance(): Float {
        val base = 0.0002f   
        val target = 0.012f  
        val s = (stageNumber - 1).coerceAtLeast(0)
        val maxStage = 100f
        val progress = (s.toFloat() / (maxStage - 1f)).coerceIn(0f, 1f)
        
        val p = 1.5f
        val t = Math.pow(progress.toDouble(), p.toDouble()).toFloat()
        return (base + (target - base) * t).coerceAtMost(target)
    }

    private data class Drone(
        var angleOffset: Float,
        var fireTimer: Float = 0f
    )
    private val drones = mutableListOf<Drone>()
    private var droneOrbitAngle = 0f
    private var laserOrbitAngle = 0f

    private data class LaserDrone(
        var angleOffset: Float,
        var cooldown: Float = 0f,
        var beamTime: Float = 0f,
        var tickTimer: Float = 0f,
        var tickCount: Int = 0
    )
    private val laserDrones = mutableListOf<LaserDrone>()

    override fun setupUI() {
        
    }

    private fun clearEnemyBullets() {
        
        enemyBullets.clear()
    }

    private fun annihilateAllEnemies() {
        
        for (e in enemies) if (e.alive) {
            e.alive = false
        }
    }

    private fun hpForStage(n: Int): Int {
        if (n <= 1) return 1
        var hp = 1
        var s = 2
        while (s <= n) {
            
            val baseInc = if (s <= 10) (((s - 1) / 5) + 1) else (((s - 1) / 4) + 1)
            hp += baseInc
            
            if (s % 15 == 0) hp += 6
            s++
        }
        return hp
    }

    override fun handleCommonInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (gameOver) {
                game.router.goTo(com.club.ScreenId.MENU)
            } else {
                paused = !paused
            }
        }
    }

    override fun update(dt: Float) {
        if (gameOver) {
            
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                resetGame()
            } else if (Gdx.input.justTouched()) {
                val v = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
                stage.camera.unproject(v)
                val (retry, menu) = gameOverButtons()
                if (pointInRect(v.x, v.y, retry)) {
                    resetGame()
                } else if (pointInRect(v.x, v.y, menu)) {
                    game.router.goTo(com.club.ScreenId.MENU)
                }
            }
            return
        }

        
        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                paused = false
            } else if (Gdx.input.justTouched()) {
                val v = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
                stage.camera.unproject(v)
                val (contBtn, retryBtn, menuBtn) = pauseButtons()
                if (pointInRect(v.x, v.y, contBtn)) {
                    paused = false
                } else if (pointInRect(v.x, v.y, retryBtn)) {
                    resetGame()
                    paused = false
                } else if (pointInRect(v.x, v.y, menuBtn)) {
                    paused = false
                    game.router.goTo(com.club.ScreenId.MENU)
                }
            }
            return
        }

        val step = dt.coerceAtMost(1f / 30f)

        handleMovement(step)

        
        if (qCd > 0f) qCd -= step
        if (wCd > 0f) wCd -= step
        if (eCd > 0f) eCd -= step
        if (rCd > 0f) rCd -= step
        if (iFrames > 0f) iFrames -= step
        if (iFrames > 0f) blinkTimer += step else blinkTimer = 0f
        if (upgradeMsgTimer > 0f) upgradeMsgTimer -= step
        if (enemyShootDisabledTimer > 0f) enemyShootDisabledTimer -= step

        
        handleSkillInput()
        updateSkills(step)

        
        firing = Gdx.input.isKeyPressed(Input.Keys.SPACE)
        fireBullets(step)

        
        updateBullets(step)
        updateEnemyBullets(step)
        updateMissiles(step)
        updateDrones(step)
        updateLaserDrones(step)
        spawnEnemies(step)
        updateEnemies(step)
        updateUpgradeKits(step)
        checkCollisions()

        
        if (stageKills >= stageTarget) {
            val clearedStage = stageNumber
            
            score += 10 * clearedStage
            if (playerHearts < playerMaxHearts) playerHearts += 1 else score += 50
            
            if (clearedStage % 2 == 1) {
                val inc = atkBonusForStage(clearedStage)
                playerAtk += inc
                upgradeMsg = "공격력 +$inc"
                upgradeMsgTimer = 2.5f
            }
            
            enemies.clear()

            stageNumber += 1
            stageKills = 0
            stageTarget = requiredKillsForStage(stageNumber)
            
            val nextTargetSpawnInterval = targetSpawnIntervalForStage(stageNumber)
            spawnInterval = nextTargetSpawnInterval

            
            if (clearedStage % 2 == 0) {
                val parts = mutableListOf<String>()
                val upMsg = applyRandomUpgrade(clearedStage)
                if (upMsg != null) parts += "업그레이드: $upMsg"
                
                if (clearedStage == 2) {
                    playerAtk += 2
                    parts += "공격력 +2"
                } else if (clearedStage == 4) {
                    playerAtk += 2
                    parts += "공격력 +2"
                }
                if (parts.isNotEmpty()) {
                    upgradeMsg = parts.joinToString(" · ")
                    upgradeMsgTimer = 2.5f
                }
            }
        }
    }

    private fun handleMovement(dt: Float) {
        val move = Vector2(0f, 0f)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) move.x -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) move.x += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) move.y += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) move.y -= 1f
        if (move.len2() > 0f) move.nor()
        val spd = playerSpeed + 20f * u5MoveSpeed
        playerPos.mulAdd(move, spd * dt)
        
        playerPos.x = playerPos.x.coerceIn(playerRadius, worldW - playerRadius)
        playerPos.y = playerPos.y.coerceIn(playerRadius, worldH - playerRadius)
    }

    override fun draw(alpha: Float) {
        
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        
        shape.projectionMatrix = stage.camera.combined
        val oldProj = game.spriteBatch.projectionMatrix.cpy()
        game.spriteBatch.projectionMatrix = stage.camera.combined

        
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(0.06f, 0.07f, 0.12f, 1f)
        shape.rect(0f, 0f, worldW, worldH)

        
        if (qActive) {
            shape.color = Color(0.5f, 0.95f, 1f, 0.55f)
            val laserW = 48f
            shape.rect(playerPos.x - laserW * 0.5f, playerPos.y + playerRadius, laserW, worldH - (playerPos.y + playerRadius))
        }

        
        for (b in bullets) if (b.alive) {
            
            shape.color = if (b.fromDrone) {
                if (u7DroneLevel >= 15) Color(0.20f, 0.85f, 1f, 1f) else Color(0.3f, 0.95f, 1f, 1f)
            } else Color(0.95f, 0.95f, 1f, 1f)
            shape.circle(b.pos.x, b.pos.y, b.radius)
        }

        
        shape.color = Color(1f, 0.5f, 0.45f, 1f)
        for (eb in enemyBullets) if (eb.alive) {
            shape.circle(eb.pos.x, eb.pos.y, eb.radius)
        }

        
        shape.color = Color(0.9f, 0.35f, 0.45f, 1f)
        for (e in enemies) if (e.alive) {
            shape.circle(e.pos.x, e.pos.y, e.radius)
        }

        
        if (upgradeKits.isNotEmpty()) {
            shape.color = Color(1f, 0.92f, 0.35f, 1f)
            for (k in upgradeKits) if (k.alive) {
                shape.circle(k.pos.x, k.pos.y, k.radius)
            }
        }

        
        if (u7DroneLevel > 0) {
            val count = droneCountForLevel(u7DroneLevel)
            val r = droneOrbitRadius(u7DroneLevel)
            for (i in 0 until count) {
                val ang = droneOrbitAngle + (MathUtils.PI2 * i) / kotlin.math.max(1, count)
                val x = playerPos.x + MathUtils.cos(ang) * r
                val y = playerPos.y + MathUtils.sin(ang) * r
                shape.color = if (u7DroneLevel >= 15) Color(1f, 0.82f, 0.30f, 1f) else Color(1f, 0.95f, 0.4f, 1f)
                shape.circle(x, y, 8f)
            }
        }

        
        if (u8LaserDroneLevel > 0) {
            val d = laserDrones.getOrNull(0)
            val orbitR = playerRadius + 46f
            val ang = (d?.angleOffset ?: 0f) + laserOrbitAngle
            val ox = playerPos.x + MathUtils.cos(ang) * orbitR
            val oy = playerPos.y + MathUtils.sin(ang) * orbitR
            
            if (d != null && d.beamTime > 0f) {
                
                val target = nearestEnemy(ox, oy)
                var tx = ox
                var ty = oy + 1f
                if (target != null) { tx = target.pos.x; ty = target.pos.y }
                val dirx = tx - ox
                val diry = ty - oy
                val len = kotlin.math.max(1f, kotlin.math.sqrt(dirx * dirx + diry * diry))
                val nx = dirx / len
                val ny = diry / len
                val beamLen = 1600f
                val ex = ox + nx * beamLen
                val ey = oy + ny * beamLen
                
                val beamColor = if (u8LaserDroneLevel >= 15) Color(1f, 0.78f, 0.18f, 0.65f) else Color(1f, 0.90f, 0.20f, 0.58f)
                shape.color = beamColor
                shape.rectLine(ox, oy, ex, ey, 12f)
            }
            
            shape.color = if (u8LaserDroneLevel >= 15) Color(1f, 0.70f, 0.18f, 1f) else Color(1f, 0.8f, 0.25f, 1f)
            shape.circle(ox, oy, 8f)
        }

        
        if (missiles.isNotEmpty()) {
            val missileColor = if (u6MissileLevel >= 15) Color(1f, 0.55f, 0.15f, 1f) else Color(1f, 0.65f, 0.2f, 1f)
            shape.color = missileColor
            for (m in missiles) if (m.alive) {
                shape.circle(m.pos.x, m.pos.y, m.radius)
            }
        }

        
        val showPlayer = iFrames <= 0f || ((blinkTimer * 20f).toInt() % 2 == 0)
        if (wActive) {
            
            shape.color = Color(1f, 0.92f, 0.25f, 0.9f)
            shape.circle(playerPos.x, playerPos.y, playerRadius + 10f)
            
            shape.color = Color(0.06f, 0.07f, 0.12f, 1f)
            shape.circle(playerPos.x, playerPos.y, playerRadius + 7f)
        }
        if (showPlayer) {
            shape.color = Color(0.9f, 0.95f, 1f, 1f)
            shape.circle(playerPos.x, playerPos.y, playerRadius)
        }
        shape.end()

        
        drawHud()

        
        if (gameOver) {
            drawGameOverUI()
        } else if (paused) {
            drawPauseMenuUI()
        }

        game.spriteBatch.projectionMatrix = oldProj
    }

    private fun drawHud() {
        val batch = game.spriteBatch
        val font = game.font
        val h = worldH

        
        shape.begin(ShapeRenderer.ShapeType.Filled)
        val heartR = 10f
        val hy = h - 24f
        var hx = 20f
        for (i in 0 until playerMaxHearts) {
            val filled = i < playerHearts
            shape.color = if (filled) Color(1f, 0.3f, 0.35f, 1f) else Color(0.3f, 0.18f, 0.2f, 1f)
            shape.circle(hx, hy, heartR)
            hx += heartR * 2f + 6f
        }
        shape.end()

        
        batch.begin()
        font.draw(batch, "점수: $score", 20f, h - 52f)
        val totalAtk = playerAtk + u9AtkBonus
        font.draw(batch, "공격력: $totalAtk", 20f, h - 76f)
        font.draw(batch, "적 HP: ${hpForStage(stageNumber)}", 20f, h - 100f)
        batch.end()

        
        val barW = 560f
        val barH = 14f
        val barX = (worldW - barW) / 2f
        val barY = h - 36f
        val ratio = (stageKills.toFloat() / stageTarget.toFloat()).coerceIn(0f, 1f)
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.35f)
        shape.rect(barX, barY, barW, barH)
        shape.color = Color(0.2f, 0.8f, 0.3f, 1f)
        shape.rect(barX, barY, barW * ratio, barH)
        shape.end()

        
        batch.begin()
        layout.setText(font, "스테이지 $stageNumber")
        val tx = barX + (barW - layout.width) / 2f
        val ty = barY - 6f
        font.draw(batch, layout, tx, ty)
        
        val progressText = "${stageKills}/${stageTarget}"
        layout.setText(font, progressText)
        font.draw(batch, layout, barX + (barW - layout.width) / 2f, barY + barH - 2f)
        batch.end()

        
        if (upgradeMsgTimer > 0f && upgradeMsg != null) {
            val msg = upgradeMsg!!
            val bx = game.spriteBatch
            val f = game.font
            bx.begin()
            layout.setText(f, msg)
            f.color = Color(1f, 0.95f, 0.6f, 1f)
            f.draw(bx, layout, (worldW - layout.width) / 2f, worldH * 0.6f)
            bx.end()
        }

        
        val cell = 50f
        val pad = 8f
        val baseX = worldW - (cell * 4 + pad * 3) - 20f
        val baseY = 20f
        val qUnlocked = stageNumber >= 5
        val wUnlocked = stageNumber >= 10
        val eUnlocked = stageNumber >= 15
        val rUnlocked = stageNumber >= 20
        drawSkillCell(baseX + 0 * (cell + pad), baseY, cell, 'Q', qCd, 30f, qUnlocked)
        drawSkillCell(baseX + 1 * (cell + pad), baseY, cell, 'W', wCd, 50f, wUnlocked)
        drawSkillCell(baseX + 2 * (cell + pad), baseY, cell, 'E', eCd, 75f, eUnlocked)
        drawSkillCell(baseX + 3 * (cell + pad), baseY, cell, 'R', rCd, 120f, rUnlocked)

        
        if (debugEnabled) {
            val bx = game.spriteBatch
            val f = game.font
            val lines = mutableListOf<String>()
            lines.add("직선타:${u1StraightCount}  사선:${u2DiagonalLevel}  탄크기:${u3BulletSize}  관통:${u4PierceLevel}")
            lines.add("이속:${u5MoveSpeed}  미사일:${u6MissileLevel}  보조 드론:${u7DroneLevel}  레이저 드론:${u8LaserDroneLevel}  ATK+:${u9AtkBonus}")
            lines.add("ATK total: ${playerAtk + u9AtkBonus} (base:$playerAtk, bonus:$u9AtkBonus)")
            val missNext = if (u6MissileLevel > 0) kotlin.math.max(0f, missileSpawnTimer) else -1f
            val droneNext = if (u7DroneLevel > 0) drones.minByOrNull { it.fireTimer }?.fireTimer?.coerceAtLeast(0f) ?: -1f else -1f
            
            var laserLine = "레이저 드론: N/A"
            if (u8LaserDroneLevel > 0) {
                val d = laserDrones.getOrNull(0)
                laserLine = if (d == null) {
                    "레이저 드론: N/A"
                } else if (d.beamTime > 0f) {
                    "레이저 드론 조사: ${"%.2fs".format(d.beamTime.coerceAtLeast(0f))} (틱:${d.tickCount})"
                } else {
                    "레이저 드론 대기: ${"%.2fs".format(d.cooldown.coerceAtLeast(0f))}"
                }
            }
            lines.add("Missile next: ${if (missNext >= 0f) "%.2fs".format(missNext) else "N/A"}  Drone next: ${if (droneNext >= 0f) "%.2fs".format(droneNext) else "N/A"}")
            lines.add(laserLine)
            
            val kitChance = upgradeKitChance()
            lines.add("업그레이드 킷 확률: ${"%.4f".format(kitChance * 100f)}%")
            lines.add("Q:${"%.1f".format(qCd)} W:${"%.1f".format(wCd)} E:${"%.1f".format(eCd)} R:${"%.1f".format(rCd)}")
            bx.begin()
            var y = 140f
            for (s in lines) {
                layout.setText(f, s)
                f.draw(bx, layout, 20f, y)
                y += 18f
            }
            bx.end()
        }
    }

    private fun handleSkillInput() {
        
        if ((Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) && Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            debugEnabled = !debugEnabled
        }
        
        if (debugEnabled && Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            val msg = applyRandomUpgrade()
            if (msg != null) {
                upgradeMsg = "업그레이드: $msg"
                upgradeMsgTimer = 2.5f
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && stageNumber >= 5 && qCd <= 0f && !wActive) {
            
            wActive = true
            wTimeLeft = 10f
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) && stageNumber >= 10 && wCd <= 0f) {
            
            wCd = 50f
            qActive = true
            qActiveTime = 4.0f
            qTickTimer = 0f
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && stageNumber >= 15 && eCd <= 0f) {
            clearEnemyBullets()
            enemyShootDisabledTimer = 8f
            eCd = 75f
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && stageNumber >= 20 && rCd <= 0f) {
            annihilateAllEnemies()
            rCd = 120f
        }
    }

    private fun updateSkills(dt: Float) {
        if (qActive) {
            qActiveTime -= dt
            qTickTimer += dt
            
            run {
                val maxTicks = 12
                var processed = 0
                while (qTickTimer >= qTickInterval && processed < maxTicks) {
                    qTickTimer -= qTickInterval
                    applyLaserTickDamage()
                    processed++
                }
            }
            if (qActiveTime <= 0f) {
                qActive = false
            }
        }
        if (wActive) {
            wTimeLeft -= dt
            if (wTimeLeft <= 0f) {
                wActive = false
                qCd = 30f
            }
        }
    }

    private fun applyLaserTickDamage() {
        
        val half = 24f
        val totalAtk = playerAtk + u9AtkBonus
        val tickDmg = kotlin.math.max(1, totalAtk * 5)
        for (e in enemies) if (e.alive) {
            if (e.pos.y > playerPos.y && kotlin.math.abs(e.pos.x - playerPos.x) <= half + e.radius) {
                e.hp -= tickDmg
                if (e.hp <= 0) {
                    onEnemyKilled(e)
                }
            }
        }
    }

    
    private fun diagonalAngleForLevel(level: Int): Float = when (level) {
        1 -> 10f   
        2 -> -10f  
        3 -> 18f   
        4 -> -18f  
        else -> 0f
    }
    private fun angleLabel(angDeg: Float): String {
        val side = if (angDeg < 0f) "우" else "좌"
        val deg = kotlin.math.abs(angDeg).toInt()
        return "${side}${deg}°"
    }
    private fun collectedDiagonalAngles(level: Int): List<Float> {
        val list = mutableListOf<Float>()
        for (lv in 1..level) list.add(diagonalAngleForLevel(lv))
        return list
    }

    private fun fireBullets(dt: Float) {
        if (fireCooldown > 0f) fireCooldown -= dt
        if (!firing || fireCooldown > 0f) return

        val baseSpeed = 780f
        val baseRadius = 8f + 2f * u3BulletSize
        val basePierce = u4PierceLevel

        
        val straightCount = 1 + u1StraightCount
        val spacing = 26f
        val start = -(straightCount - 1) * 0.5f
        for (i in 0 until straightCount) {
            val offset = (start + i) * spacing
            val b = Bullet(
                pos = Vector2(playerPos.x + offset, playerPos.y + playerRadius + 6f),
                vel = Vector2(0f, baseSpeed),
                radius = baseRadius,
                dmg = playerAtk + u9AtkBonus,
                alive = true,
                pierce = basePierce
            )
            bullets.add(b)
        }

        
        if (u2DiagonalLevel > 0) {
            val suppressDiag = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
            if (!suppressDiag) {
                val diagSpeed = if (stageNumber < 20) baseSpeed * 0.85f else baseSpeed
                val angles = collectedDiagonalAngles(u2DiagonalLevel)
                for (ang in angles) {
                    val dir = Vector2(0f, 1f).rotateDeg(ang)
                    bullets.add(
                        Bullet(
                            pos = Vector2(playerPos.x, playerPos.y + playerRadius + 6f),
                            vel = Vector2(dir.x * diagSpeed, dir.y * diagSpeed),
                            radius = baseRadius,
                            dmg = playerAtk + u9AtkBonus,
                            alive = true,
                            pierce = basePierce
                        )
                    )
                }
            }
        }

        
        fireCooldown = fireInterval
    }

    private fun updateBullets(dt: Float) {
        var i = bullets.size - 1
        while (i >= 0) {
            val b = bullets[i]
            if (!b.alive) {
                bullets.removeAt(i)
            } else {
                b.pos.mulAdd(b.vel, dt)
                
                if (b.pos.y - b.radius > worldH + 12f || b.pos.y + b.radius < -12f || b.pos.x + b.radius < -12f || b.pos.x - b.radius > worldW + 12f) {
                    bullets.removeAt(i)
                }
            }
            i--
        }
    }

    private fun updateEnemyBullets(dt: Float) {
        var i = enemyBullets.size - 1
        while (i >= 0) {
            val eb = enemyBullets[i]
            if (!eb.alive) {
                enemyBullets.removeAt(i)
            } else {
                if (eb.homing) {
                    val desired = Vector2(playerPos.x - eb.pos.x, playerPos.y - eb.pos.y)
                    if (desired.len2() > 1f) desired.nor()
                    val spd = eb.vel.len().coerceAtLeast(enemyHomingSpeed())
                    val desiredVel = Vector2(desired.x * spd, desired.y * spd)
                    eb.vel.lerp(desiredVel, 0.06f)
                    eb.life -= dt
                    if (eb.life <= 0f) { eb.alive = false; enemyBullets.removeAt(i); i--; continue }
                }
                eb.pos.mulAdd(eb.vel, dt)
                if (eb.pos.y + eb.radius < -12f || eb.pos.y - eb.radius > worldH + 12f || eb.pos.x + eb.radius < -12f || eb.pos.x - eb.radius > worldW + 12f) {
                    enemyBullets.removeAt(i)
                }
            }
            i--
        }
    }

    
    private fun enemyStraightCap(): Int = when {
        stageNumber <= 10 -> 10
        stageNumber <= 20 -> 14
        stageNumber <= 30 -> 18
        else -> 20
    }
    private fun enemyDiagonalCap(): Int = when {
        stageNumber < 10 -> 0
        stageNumber <= 20 -> 4
        stageNumber <= 30 -> 8
        else -> 10
    }
    private fun enemyHomingCap(): Int = if (stageNumber >= 41) 5 else 0
    private fun enemyStraightSpeed(): Float = when {
        stageNumber <= 10 -> 480f
        stageNumber <= 20 -> 500f
        stageNumber <= 30 -> 520f
        else -> 540f
    }
    private fun enemyDiagonalSpeed(): Float = when {
        stageNumber < 10 -> 0f
        stageNumber <= 20 -> 340f
        stageNumber <= 30 -> 380f
        else -> 400f
    }
    private fun enemyHomingSpeed(): Float = 160f
    private fun enemyHomingLife(): Float = if (stageNumber >= 41) 5.0f else 0f
    private fun enemyBulletCount(type: Int): Int = enemyBullets.count { it.alive && it.type == type }
    private fun canSpawnEnemyBulletType(type: Int): Boolean {
        val cap = when (type) {
            0 -> enemyStraightCap()
            1 -> enemyDiagonalCap()
            else -> enemyHomingCap()
        }
        return enemyBulletCount(type) < cap
    }

    private fun spawnEnemies(dt: Float) {
        spawnTimer += dt
        
        val targetInterval = targetSpawnIntervalForStage(stageNumber)
        spawnInterval += (targetInterval - spawnInterval) * 0.2f
        if (spawnTimer >= spawnInterval) {
            spawnTimer -= spawnInterval
            val options = when {
                stageNumber < 5 -> mutableListOf(0, 1) 
                stageNumber < 10 -> mutableListOf(0, 1, 2, 3) 
                stageNumber < 15 -> mutableListOf(0, 1, 2, 3, 4) 
                stageNumber < 20 -> mutableListOf(0, 1, 2, 3, 4, 6) 
                stageNumber < 25 -> mutableListOf(0, 1, 2, 3, 4, 6, 9) 
                stageNumber < 30 -> mutableListOf(0, 1, 2, 3, 4, 6, 7, 9) 
                stageNumber < 35 -> mutableListOf(0, 1, 2, 3, 4, 6, 7, 9, 10) 
                else -> mutableListOf(0, 1, 2, 3, 4, 6, 7, 8, 9, 10) 
            }
            when (options[MathUtils.random(0, options.size - 1)]) {
                0 -> spawnSingleDown()
                1 -> spawnWaveFan()
                2 -> spawnWaveHLine()
                3 -> spawnWaveSnake()
                4 -> spawnWaveDiamond()
                6 -> spawnWaveWideFan()
                7 -> spawnWaveSquareGrid()
                8 -> spawnWaveRing()
                9 -> spawnWaveSquarePerimeter()
                10 -> spawnWaveLongFan()
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        var i = enemies.size - 1
        while (i >= 0) {
            val e = enemies[i]
            if (!e.alive) {
                enemies.removeAt(i)
            } else {
                e.time += dt
                
                when (e.pattern) {
                    0 -> { 
                        e.pos.mulAdd(e.vel, dt)
                    }
                    1 -> { 
                        e.pos.mulAdd(e.vel, dt)
                    }
                    2 -> { 
                        e.pos.mulAdd(e.vel, dt)
                        if (e.pos.x <= e.radius || e.pos.x >= worldW - e.radius) e.vel.x = -e.vel.x
                    }
                    3 -> { 
                        e.pos.y += e.vel.y * dt
                        e.pos.x = e.baseX + MathUtils.sin(e.time * e.omega) * e.amp
                    }
                    4 -> { 
                        e.pos.mulAdd(e.vel, dt)
                    }
                    5 -> { 
                        e.pos.mulAdd(e.vel, dt)
                    }
                }

                
                val nearEdge = (e.pos.x <= e.radius || e.pos.x >= worldW - e.radius || e.pos.y <= e.radius || e.pos.y >= worldH - e.radius)
                if (nearEdge) e.edgeTime += dt else e.edgeTime = 0f
                if (e.edgeTime >= 2f) {
                    enemies.removeAt(i)
                    i--
                    continue
                }

                
                e.shootTimer -= dt
                if (e.shootTimer <= 0f) {
                    tryEnemyShoot(e)
                }

                if (e.pos.y + e.radius < -8f) {
                    enemies.removeAt(i)
                }
            }
            i--
        }
    }

    private fun checkCollisions() {
        
        var bi = bullets.size - 1
        while (bi >= 0) {
            val b = bullets[bi]
            if (!b.alive) { bullets.removeAt(bi); bi--; continue }
            var ei = enemies.size - 1
            while (ei >= 0) {
                val e = enemies[ei]
                if (!e.alive) { ei--; continue }
                if (circlesOverlap(b.pos.x, b.pos.y, b.radius, e.pos.x, e.pos.y, e.radius)) {
                    
                    if (!b.hit.contains(e.id)) {
                        b.hit.add(e.id)
                        val dealt = b.dmg
                        e.hp -= dealt
                        if (e.hp <= 0) {
                            onEnemyKilled(e)
                        }
                        if (b.pierce > 0) {
                            b.pierce -= 1
                            
                        } else {
                            b.alive = false
                            break
                        }
                    }
                }
                ei--
            }
            if (!b.alive) bullets.removeAt(bi)
            bi--
        }

        
        var mi = missiles.size - 1
        while (mi >= 0) {
            val m = missiles[mi]
            if (!m.alive) { missiles.removeAt(mi); mi--; continue }
            var ei2 = enemies.size - 1
            while (ei2 >= 0) {
                val e = enemies[ei2]
                if (!e.alive) { ei2--; continue }
                if (circlesOverlap(m.pos.x, m.pos.y, m.radius, e.pos.x, e.pos.y, e.radius)) {
                    
                    val dmg = if (m.dmg > 0) m.dmg else (playerAtk + u9AtkBonus) + missileDamageOffset(u6MissileLevel)
                    e.hp -= dmg
                    if (e.hp <= 0) {
                        onEnemyKilled(e)
                    }
                    m.alive = false
                    break
                }
                ei2--
            }
            if (!m.alive) missiles.removeAt(mi)
            mi--
        }

        
        if (iFrames <= 0f) {
            for (e in enemies) if (e.alive) {
                if (circlesOverlap(playerPos.x, playerPos.y, playerRadius, e.pos.x, e.pos.y, e.radius)) {
                    if (wActive) {
                        
                        
                        wActive = false
                        wTimeLeft = 0f
                        qCd = 30f
                        iFrames = 0.5f
                        
                        val dx = e.pos.x - playerPos.x
                        val dy = e.pos.y - playerPos.y
                        val len = kotlin.math.max(1f, kotlin.math.sqrt(dx * dx + dy * dy))
                        val pushDist = playerRadius + e.radius + 6f
                        e.pos.x = playerPos.x + (dx / len) * pushDist
                        e.pos.y = playerPos.y + (dy / len) * pushDist
                    } else {
                        
                        e.alive = false
                        playerHearts -= 1
                        iFrames = 1.5f
                        if (playerHearts <= 0) {
                            gameOver = true
                            paused = false
                        }
                        break
                    }
                }
            }
        }

        
        if (iFrames <= 0f) {
            var i = enemyBullets.size - 1
            while (i >= 0) {
                val eb = enemyBullets[i]
                if (!eb.alive) { enemyBullets.removeAt(i); i--; continue }
                if (circlesOverlap(playerPos.x, playerPos.y, playerRadius, eb.pos.x, eb.pos.y, eb.radius)) {
                    if (wActive) {
                        wActive = false
                        wTimeLeft = 0f
                        qCd = 30f
                        iFrames = 0.5f
                        eb.alive = false
                        enemyBullets.removeAt(i)
                    } else {
                        eb.alive = false
                        enemyBullets.removeAt(i)
                        playerHearts -= 1
                        iFrames = 1.5f
                        if (playerHearts <= 0) { gameOver = true; paused = false }
                    }
                }
                i--
            }
        }
    }

    private fun circlesOverlap(ax: Float, ay: Float, ar: Float, bx: Float, by: Float, br: Float): Boolean {
        val dx = ax - bx
        val dy = ay - by
        val rr = ar + br
        return dx * dx + dy * dy <= rr * rr
    }

    
    private fun segmentIntersectsCircle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, cr: Float): Boolean {
        val abx = bx - ax
        val aby = by - ay
        val acx = cx - ax
        val acy = cy - ay
        val abLen2 = abx * abx + aby * aby
        if (abLen2 <= 1e-6f) {
            
            val dx = ax - cx
            val dy = ay - cy
            return dx * dx + dy * dy <= cr * cr
        }
        var t = (acx * abx + acy * aby) / abLen2
        if (t < 0f) t = 0f
        else if (t > 1f) t = 1f
        val px = ax + abx * t
        val py = ay + aby * t
        val dx = px - cx
        val dy = py - cy
        return dx * dx + dy * dy <= cr * cr
    }

    private fun resetGame() {
        
        playerPos.set(worldW * 0.5f, worldH * 0.15f)
        playerHearts = 4
        playerMaxHearts = 6
        playerAtk = 1
        iFrames = 0f
        blinkTimer = 0f

        
        bullets.clear()
        enemies.clear()
        enemyBullets.clear()
        missiles.clear()
        drones.clear()
        laserDrones.clear()
        upgradeKits.clear()

        
        score = 0
        stageNumber = 1
        stageKills = 0
        stageTarget = requiredKillsForStage(stageNumber)
        spawnTimer = 0f
        
        val initialSpawnInterval = targetSpawnIntervalForStage(stageNumber)
        spawnInterval = initialSpawnInterval

        
        qCd = 0f; wCd = 0f; eCd = 0f; rCd = 0f
        qActive = false; qActiveTime = 0f; qTickTimer = 0f
        wActive = false; wTimeLeft = 0f
        upgradeMsg = null; upgradeMsgTimer = 0f
        u1StraightCount = 0; u2DiagonalLevel = 0; u3BulletSize = 0; u4PierceLevel = 0; u5MoveSpeed = 0; u9AtkBonus = 0
        u6MissileLevel = 0; u7DroneLevel = 0; u8LaserDroneLevel = 0
        missileSpawnTimer = 0f; droneOrbitAngle = 0f
        
        laserOrbitAngle = 0f
        

        
        firing = false
        fireCooldown = 0f
        gameOver = false
        paused = false
    }

    private fun drawSkillCell(x: Float, y: Float, size: Float, key: Char, cdLeft: Float, cdMax: Float, enabled: Boolean) {
        
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.12f, 0.14f, 0.18f, 1f)
        shape.rect(x, y, size, size)
        
        if (enabled && cdLeft > 0f) {
            val ratio = (cdLeft / cdMax).coerceIn(0f, 1f)
            shape.color = Color(0f, 0f, 0f, 0.45f)
            shape.rect(x, y, size, size * ratio)
        } else if (!enabled) {
            shape.color = Color(0f, 0f, 0f, 0.55f)
            shape.rect(x, y, size, size)
        }
        shape.end()
        
        val batch = game.spriteBatch
        val font = game.font
        batch.begin()
        layout.setText(font, key.toString())
        font.color = if (enabled) Color.WHITE else Color(0.6f, 0.6f, 0.6f, 1f)
        font.draw(batch, layout, x + (size - layout.width) / 2f, y + size - 8f)
        if (enabled && cdLeft > 0f) {
            val secs = MathUtils.ceil(cdLeft).toString()
            layout.setText(font, secs)
            font.draw(batch, layout, x + (size - layout.width) / 2f, y + 18f)
        }
        batch.end()
    }

    private fun requiredKillsForStage(n: Int): Int {
        if (n == 1) return 20
        val extra = n + (n / 5)
        return 12 + 8 * extra
    }

    override fun dispose() {
        super.dispose()
        shape.dispose()
    }

    
    private fun baseEnemyHp(): Int = hpForStage(stageNumber)
    private fun baseDownVel(): Vector2 {
        val steps = (stageNumber - 1) / 5
        val vy = - (120f * (1f + 0.02f * steps))
        return Vector2(0f, vy + MathUtils.random(-8f, 8f))
    }

    private fun targetSpawnIntervalForStage(n: Int): Float {
        val base = (1.02f - 0.03f * (n - 1)).coerceAtLeast(0.45f)
        val boost = when {
            n <= 3 -> 1.4f
            n <= 6 -> 1.25f
            n <= 10 -> 1.1f
            else -> 1f
        }
        return (base * boost).coerceIn(0.45f, 1.8f)
    }

    private fun maybeSpecial(kindProb: Float): Int {
        
        return 1
    }

    private fun spawnSingleDown() {
        val r = 18f + MathUtils.random(-2f, 6f)
        val x = MathUtils.random(r, worldW - r)
        val y = worldH + r + 6f
        val hp = baseEnemyHp()

        
        val pSpecial = kotlin.math.min(0.001f + (0.05f - 0.001f) * ((stageNumber - 1).coerceAtLeast(0) / 200f), 0.05f)
        val isSpecial = MathUtils.random() < pSpecial
        if (isSpecial) {
            when (MathUtils.random(0, 5)) {
                0 -> { 
                    val e = Enemy(pos = Vector2(x, y), vel = baseDownVel().scl(0.75f), radius = r + 4f, hp = hp * 3, alive = true, scoreValue = 4, pattern = 0)
                    e.id = nextEnemyId++; enemies.add(e)
                }
                1 -> { 
                    val e = Enemy(pos = Vector2(x, y), vel = baseDownVel(), radius = r, hp = hp + 1, alive = true, scoreValue = 3, pattern = 0, tripleShooter = true, shootTimer = 1.2f)
                    e.id = nextEnemyId++; enemies.add(e)
                }
                2 -> { 
                    val e = Enemy(pos = Vector2(x, y), vel = baseDownVel().scl(1.6f), radius = r, hp = hp, alive = true, scoreValue = 2, pattern = 0)
                    e.id = nextEnemyId++; enemies.add(e)
                }
                3 -> { 
                    val e = Enemy(pos = Vector2(x, y), vel = baseDownVel(), radius = r, hp = hp + 2, alive = true, scoreValue = 5, pattern = 0, healer = true, shootTimer = 1.2f)
                    e.id = nextEnemyId++; enemies.add(e)
                }
                4 -> { 
                    val e = Enemy(pos = Vector2(x, y), vel = baseDownVel(), radius = r, hp = hp + 1, alive = true, scoreValue = 4, pattern = 0, bomber = true, shootTimer = 1.0f)
                    e.id = nextEnemyId++; enemies.add(e)
                }
                5 -> { 
                    val e = Enemy(pos = Vector2(x, y), vel = baseDownVel(), radius = r, hp = hp + 1, alive = true, scoreValue = 3, pattern = 0, splitter = true)
                    e.id = nextEnemyId++; enemies.add(e)
                }
            }
        } else {
            val e = Enemy(pos = Vector2(x, y), vel = baseDownVel(), radius = r, hp = hp, alive = true, scoreValue = 1, pattern = 0)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun spawnWaveFan() {
        val count = if (stageNumber < 5) 3 + MathUtils.random(0, 1) else 5 + MathUtils.random(0, 2)
        val centerX = worldW * 0.5f + MathUtils.random(-200f, 200f)
        val y = worldH + 24f
        val hp = baseEnemyHp()
        for (i in 0 until count) {
            val t = if (count == 1) 0f else (i - (count - 1) * 0.5f) / ((count - 1) * 0.5f)
            val ang = t * 25f
            val speed = 150f
            val dir = Vector2(0f, -1f).rotateDeg(ang)
            val e = Enemy(pos = Vector2(centerX, y), vel = Vector2(dir.x * speed, dir.y * speed), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 1)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun spawnWaveHLine() {
        val count = 6
        val fromLeft = MathUtils.randomBoolean()
        val y = worldH + 20f
        val hp = baseEnemyHp()
        val vx = if (fromLeft) 180f else -180f
        val startX = if (fromLeft) -40f else worldW + 40f
        for (i in 0 until count) {
            val x = startX + i * (if (fromLeft) 40f else -40f)
            val e = Enemy(pos = Vector2(x, y + i * 4f), vel = Vector2(vx, -60f), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 2)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun spawnWaveSnake() {
        val count = 5
        val baseX = MathUtils.random(120f, worldW - 120f)
        val y = worldH + 20f
        val hp = baseEnemyHp()
        val vy = -100f
        for (i in 0 until count) {
            val amp = MathUtils.random(60f, 120f)
            val omega = MathUtils.random(2.5f, 3.5f)
            val e = Enemy(pos = Vector2(baseX, y + i * 24f), vel = Vector2(0f, vy), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 3, baseX = baseX, amp = amp, omega = omega)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun spawnWaveDiamond() {
        val hp = baseEnemyHp()
        val cx = MathUtils.random(200f, worldW - 200f)
        val cy = worldH + 40f
        val vy = -110f
        val pts = arrayOf(
            Vector2(cx, cy + 0f),
            Vector2(cx - 26f, cy - 18f),
            Vector2(cx + 26f, cy - 18f),
            Vector2(cx - 14f, cy - 42f),
            Vector2(cx + 14f, cy - 42f),
        )
        for (p in pts) {
            val e = Enemy(pos = Vector2(p.x, p.y), vel = Vector2(0f, vy), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 4)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun spawnWaveWideFan() {
        val count = 7 + MathUtils.random(1, 3)
        val centerX = worldW * 0.5f + MathUtils.random(-260f, 260f)
        val y = worldH + 24f
        val hp = baseEnemyHp()
        for (i in 0 until count) {
            val t = if (count == 1) 0f else (i - (count - 1) * 0.5f) / ((count - 1) * 0.5f)
            val ang = t * 45f
            val speed = 140f
            val dir = Vector2(0f, -1f).rotateDeg(ang)
            enemies.add(Enemy(pos = Vector2(centerX, y), vel = Vector2(dir.x * speed, dir.y * speed), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 1))
        }
    }

    private fun spawnWaveSquareGrid() {
        val hp = baseEnemyHp()
        val cols = 4
        val rows = 3
        val cell = 48f
        val startX = MathUtils.random(200f, worldW - 200f)
        val startY = worldH + 40f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = (startX - (cols - 1) * 0.5f * cell) + c * cell
                val y = startY + (rows - 1 - r) * 24f
                val e = Enemy(pos = Vector2(x, y), vel = Vector2(0f, -110f), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 0)
                e.id = nextEnemyId++; enemies.add(e)
            }
        }
    }

    private fun spawnWaveRing() {
        val hp = baseEnemyHp()
        val cx = MathUtils.random(260f, worldW - 260f)
        val cy = worldH + 80f
        val n = 8 + MathUtils.random(0, 2)
        val radius = 60f
        for (k in 0 until n) {
            val ang = k * (360f / n)
            val px = cx + MathUtils.cosDeg(ang) * radius
            val py = cy + MathUtils.sinDeg(ang) * radius
            val e = Enemy(pos = Vector2(px, py), vel = Vector2(0f, -120f), radius = 16f, hp = hp, alive = true, scoreValue = 1, pattern = 5)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun spawnWaveSquarePerimeter() {
        val hp = baseEnemyHp()
        val cx = MathUtils.random(220f, worldW - 220f)
        val cy = worldH + 90f
        val halfW = 120f
        val halfH = 80f
        val step = 32f
        var x = -halfW
        while (x <= halfW) {
            run {
                val e = Enemy(pos = Vector2(cx + x, cy + halfH), vel = Vector2(0f, -110f), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 0)
                e.id = nextEnemyId++; enemies.add(e)
            }
            run {
                val e = Enemy(pos = Vector2(cx + x, cy - halfH), vel = Vector2(0f, -110f), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 0)
                e.id = nextEnemyId++; enemies.add(e)
            }
            x += step
        }
        var y = -halfH + step
        while (y < halfH) {
            run {
                val e = Enemy(pos = Vector2(cx - halfW, cy + y), vel = Vector2(0f, -110f), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 0)
                e.id = nextEnemyId++; enemies.add(e)
            }
            run {
                val e = Enemy(pos = Vector2(cx + halfW, cy + y), vel = Vector2(0f, -110f), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 0)
                e.id = nextEnemyId++; enemies.add(e)
            }
            y += step
        }
    }

    private fun spawnWaveLongFan() {
        val count = 11 + MathUtils.random(0, 4)
        val centerX = worldW * 0.5f + MathUtils.random(-300f, 300f)
        val y = worldH + 24f
        val hp = baseEnemyHp()
        for (i in 0 until count) {
            val t = if (count == 1) 0f else (i - (count - 1) * 0.5f) / ((count - 1) * 0.5f)
            val ang = t * 60f
            val speed = 150f
            val dir = Vector2(0f, -1f).rotateDeg(ang)
            val e = Enemy(pos = Vector2(centerX, y), vel = Vector2(dir.x * speed, dir.y * speed), radius = 18f, hp = hp, alive = true, scoreValue = 1, pattern = 1)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    private fun tryEnemyShoot(e: Enemy) {
        if (enemyShootDisabledTimer > 0f) {
            e.shootTimer = MathUtils.random(0.6f, 1.2f)
            return
        }
        e.shootTimer = MathUtils.random(1.0f, 1.6f)
        val bulletSpeed = enemyStraightSpeed()
        if (e.healer) {
            val healRadius = 160f
            var healed = 0
            for (ally in enemies) if (ally.alive && ally !== e) {
                val dx = ally.pos.x - e.pos.x
                val dy = ally.pos.y - e.pos.y
                if (dx*dx + dy*dy <= healRadius*healRadius) {
                    ally.hp += 1
                    healed += 1
                    if (healed >= 3) break
                }
            }
            return
        }
        if (e.tripleShooter) {
            if (stageNumber < 21) {
                spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 0f, bulletSpeed, 0)
                spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 0f, bulletSpeed, 0)
                spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 0f, bulletSpeed, 0)
            } else {
                spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 0f, bulletSpeed)
                spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, -12f, bulletSpeed)
                spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 12f, bulletSpeed)
            }
            return
        }
        if (e.bomber) {
            val n = 8
            val spd = 420f
            val baseX = e.pos.x
            val baseY = e.pos.y - e.radius - 2f
            for (k in 0 until n) {
                val ang = k * (360f / n)
                spawnEnemyBullet(baseX, baseY, ang, spd)
            }
            return
        }
        run {
            val homingCount = enemyBullets.count { it.alive && it.homing }
            if (stageNumber >= 41 && homingCount < enemyHomingCap() && MathUtils.random() < 0.06f) {
                spawnHomingEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f)
                return
            }
        }
        val p = kotlin.math.min(0.24f, 0.045f + 0.009f * (stageNumber - 1))
        if (MathUtils.random() < p) {
            val dx = playerPos.x - e.pos.x
            val dy = playerPos.y - e.pos.y
            val dir = Vector2(dx, dy)
            if (dir.len2() > 0f) dir.nor()
            val aimDeg = dir.angleDeg() - 270f
            val chooseDiagonal = stageNumber >= 21 && MathUtils.random() < 0.30f
            if (!chooseDiagonal) {
                if (stageNumber < 21) {
                    spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 0f, bulletSpeed, 0)
                } else {
                    spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, aimDeg, bulletSpeed, 0)
                }
            } else {
                val baseDeg = aimDeg
                val offset = MathUtils.random(-20f, 20f)
                val diagDeg = baseDeg + offset
                val diagSpeed = enemyDiagonalSpeed()
                if (diagSpeed > 0f) {
                    spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, diagDeg, diagSpeed, 1)
                } else {
                    spawnEnemyBullet(e.pos.x, e.pos.y - e.radius - 4f, 0f, bulletSpeed, 0)
                }
            }
        }
    }

    private fun spawnEnemyBullet(x: Float, y: Float, angleDeg: Float, speed: Float, type: Int = -1): Boolean {
        
        val norm = ((angleDeg % 360f) + 360f) % 360f
        val computedType = if (type >= 0) type else if (kotlin.math.abs(norm % 180f) < 0.001f) 0 else 1
        if (!canSpawnEnemyBulletType(computedType)) return false
        val dir = Vector2(0f, -1f).rotateDeg(angleDeg)
        enemyBullets.add(EnemyBullet(pos = Vector2(x, y), vel = Vector2(dir.x * speed, dir.y * speed), radius = 5f, alive = true, type = computedType))
        return true
    }

    private fun spawnHomingEnemyBullet(x: Float, y: Float, speed: Float = -1f, life: Float = -1f): Boolean {
        if (!canSpawnEnemyBulletType(2)) return false
        val spd = if (speed > 0f) speed else enemyHomingSpeed()
        val lf = if (life > 0f) life else enemyHomingLife()
        val dir = Vector2(playerPos.x - x, playerPos.y - y)
        if (dir.len2() > 0f) dir.nor()
        enemyBullets.add(
            EnemyBullet(
                pos = Vector2(x, y),
                vel = Vector2(dir.x * spd, dir.y * spd),
                radius = 6f,
                alive = true,
                homing = true,
                life = lf,
                maxLife = lf,
                type = 2
            )
        )
        return true
    }

    private fun spawnSplitterChildren(parent: Enemy) {
        val hp = 1
        val r = (parent.radius * 0.7f).coerceAtLeast(10f)
        val speed = 160f
        val angles = listOf(-20f, 20f)
        for (ang in angles) {
            val dir = Vector2(0f, -1f).rotateDeg(ang)
            val e = Enemy(pos = Vector2(parent.pos.x, parent.pos.y), vel = Vector2(dir.x * speed, dir.y * speed), radius = r, hp = hp, alive = true, scoreValue = 1, pattern = 1, child = true)
            e.id = nextEnemyId++; enemies.add(e)
        }
    }

    
    private fun applyRandomUpgrade(stageContext: Int = stageNumber): String? {
        
        data class Item(val id: Int, val w: Int)
        val items = mutableListOf<Item>()
        fun add(id: Int, w: Int) { items.add(Item(id, w)) }

        if (u1StraightCount < 4) add(1, 9)
        if (u2DiagonalLevel < 4) add(2, 9)
        if (u3BulletSize < 2) add(3, 8)
        if (u4PierceLevel < 2) add(4, 7)
        if (u5MoveSpeed < 4) add(5, 8)
        if (u6MissileLevel < 15) add(6, 8)
        if (u7DroneLevel < 15) add(7, 7)
        if (u8LaserDroneLevel < 15) add(8, 7)
        
        add(9, 6)

        
        
        
        
        val preferred = mutableListOf<Item>()
        if (stageContext == 2) {
            if (u1StraightCount < 4) preferred.add(Item(1, 9))
            if (u2DiagonalLevel < 4) preferred.add(Item(2, 9))
        } else if (stageContext == 4) {
            if (u6MissileLevel < 15) preferred.add(Item(6, 8))
            if (u7DroneLevel < 15) preferred.add(Item(7, 7))
            if (u8LaserDroneLevel < 15) preferred.add(Item(8, 7))
        }
        val pool = if (preferred.isNotEmpty()) preferred else items

        if (pool.isEmpty()) return null
        val total = pool.sumOf { it.w }
        var r = MathUtils.random(0, total - 1)
        var chosen = pool.first()
        for (it in pool) {
            if (r < it.w) { chosen = it; break } else r -= it.w
        }
        return when (chosen.id) {
            1 -> { u1StraightCount += 1; "직선탄 +1" }
            2 -> { u2DiagonalLevel += 1; "사선 탄환 한 쌍 추가" }
            3 -> { u3BulletSize += 1; "탄환 크기 증가" }
            4 -> { u4PierceLevel += 1; "관통 +1" }
            5 -> { u5MoveSpeed += 1; "이동 속도 증가" }
            6 -> { u6MissileLevel = (u6MissileLevel + 1).coerceAtMost(15); "유도 미사일 강화" }
            7 -> { u7DroneLevel = (u7DroneLevel + 1).coerceAtMost(15); ensureDroneCounts(); "보조 드론 강화" }
            8 -> { u8LaserDroneLevel = (u8LaserDroneLevel + 1).coerceAtMost(15); ensureLaserDroneCounts(); "레이저 드론 강화" }
            9 -> {
                val inc = MathUtils.random(1, 3)
                u9AtkBonus += inc
                "공격력 +$inc"
            }
            else -> null
        }
    }

    private fun onEnemyKilled(e: Enemy) {
        e.alive = false
        if (e.splitter && !e.child) {
            spawnSplitterChildren(e)
        }
        score += e.scoreValue
        stageKills += 1
        
        if (MathUtils.random() < upgradeKitChance()) {
            spawnUpgradeKit(e.pos.x, e.pos.y)
        }
    }

    
    private fun droneCountForLevel(level: Int): Int = when {
        level >= 10 -> 3
        level >= 4 -> 2
        level >= 1 -> 1
        else -> 0
    }
    private fun droneOrbitRadius(level: Int): Float = 80f
    private fun droneAngularVelocityDeg(level: Int): Float = 120f
    private fun droneFireInterval(level: Int): Float = when (level) {
        1 -> 4.0f
        2 -> 3.75f
        3 -> 3.5f
        4 -> 3.5f
        5 -> 3.25f
        6 -> 3.0f
        7 -> 2.75f
        8 -> 2.5f
        9 -> 2.25f
        10 -> 2.25f
        11 -> 2.0f
        12 -> 1.75f
        13 -> 1.5f
        14 -> 1.25f
        15 -> 1.0f
        else -> 1.0f
    }
    private fun droneDamage(totalAtk: Int, level: Int): Int = totalAtk + when (level) {
        1 -> 0
        2 -> 1
        3 -> 2
        4 -> 2
        5 -> 3
        6 -> 5
        7 -> 7
        8 -> 7
        9 -> 10
        10 -> 10
        11 -> 15
        12 -> 20
        13 -> 30
        14 -> 30
        15 -> 50
        else -> 50
    }
    private fun ensureDroneCounts() {
        
        drones.clear()
        val count = droneCountForLevel(u7DroneLevel)
        for (i in 0 until count) {
            val off = (MathUtils.PI2 * i) / kotlin.math.max(1, count)
            drones.add(Drone(off))
        }
    }
    private fun ensureLaserDroneCounts() {
        laserDrones.clear()
        if (u8LaserDroneLevel > 0) {
            laserDrones.add(LaserDrone(angleOffset = 0f))
        }
    }

    private fun nearestEnemy(x: Float, y: Float): Enemy? {
        var best: Enemy? = null
        var bestD2 = Float.MAX_VALUE
        for (e in enemies) if (e.alive) {
            val dx = e.pos.x - x
            val dy = e.pos.y - y
            val d2 = dx*dx + dy*dy
            if (d2 < bestD2) { bestD2 = d2; best = e }
        }
        return best
    }

    
    private fun missileSpawnInterval(level: Int): Float = when (level.coerceIn(1, 15)) {
        1 -> 10.0f
        2 -> 9.5f
        3 -> 9.0f
        4 -> 9.0f
        5 -> 8.5f
        6 -> 8.5f
        7 -> 8.0f
        8 -> 7.5f
        9 -> 7.0f
        10 -> 6.5f
        11 -> 6.5f
        12 -> 6.0f
        13 -> 5.5f
        14 -> 5.0f
        else -> 5.0f 
    }
    private fun missileCountForLevel(level: Int): Int = when {
        level >= 15 -> 5
        level >= 11 -> 4
        level >= 6 -> 3
        level >= 4 -> 2
        level >= 1 -> 1
        else -> 0
    }
    private fun missileDamageOffset(level: Int): Int = when {
        level >= 15 -> 50
        level >= 14 -> 30
        level >= 13 -> 20
        level >= 12 -> 15
        level >= 11 -> 15
        level >= 8 -> 10
        level >= 7 -> 7
        level >= 5 -> 5
        level >= 3 -> 3
        level >= 2 -> 2
        level >= 1 -> 1
        else -> 0
    }

    private fun updateMissiles(dt: Float) {
        
        if (u6MissileLevel > 0) {
            missileSpawnTimer -= dt
            val lvl = u6MissileLevel.coerceIn(1, 15)
            val interval = missileSpawnInterval(lvl)
            if (missileSpawnTimer <= 0f) {
                missileSpawnTimer = interval
                val count = missileCountForLevel(lvl)
                val stepAng = if (count > 0) 360f / count else 0f
                val startAng = 90f 
                val spawnR = playerRadius + 12f
                val totalAtk = playerAtk + u9AtkBonus
                val dmgVal = totalAtk + missileDamageOffset(lvl)
                for (i in 0 until count) {
                    val ang = startAng + stepAng * i
                    val dirx = MathUtils.cosDeg(ang)
                    val diry = MathUtils.sinDeg(ang)
                    val sx = playerPos.x + dirx * spawnR
                    val sy = playerPos.y + diry * spawnR
                    val m = Missile(
                        pos = Vector2(sx, sy),
                        vel = Vector2(dirx * 650f, diry * 650f),
                        radius = 8f,
                        alive = true,
                        angleDeg = ang,
                        life = 3f,
                        hasRetargeted = false,
                        targetId = nearestEnemy(sx, sy)?.id ?: 0,
                        dmg = dmgVal
                    )
                    missiles.add(m)
                }
            }
        }

        
        var i = missiles.size - 1
        val speed = 650f
        val rotSpeed = 360f 
        while (i >= 0) {
            val m = missiles[i]
            if (!m.alive) { missiles.removeAt(i); i--; continue }

            
            var targ: Enemy? = null
            if (m.targetId != 0) {
                for (e in enemies) if (e.alive && e.id == m.targetId) { targ = e; break }
            }
            if ((targ == null || !targ.alive) && !m.hasRetargeted) {
                val nt = nearestEnemy(m.pos.x, m.pos.y)
                if (nt != null) {
                    m.targetId = nt.id
                    m.hasRetargeted = true
                    targ = nt
                }
            }

            
            val desiredAngle = if (targ != null) {
                val dx = targ.pos.x - m.pos.x
                val dy = targ.pos.y - m.pos.y
                MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees
            } else m.angleDeg
            var delta = ((desiredAngle - m.angleDeg + 540f) % 360f) - 180f
            val maxTurn = rotSpeed * dt
            if (kotlin.math.abs(delta) <= maxTurn) m.angleDeg = desiredAngle else m.angleDeg += MathUtils.clamp(delta, -maxTurn, maxTurn)

            
            val dirx = MathUtils.cosDeg(m.angleDeg)
            val diry = MathUtils.sinDeg(m.angleDeg)
            m.vel.set(dirx * speed, diry * speed)
            m.pos.mulAdd(m.vel, dt)

            
            m.life -= dt
            if (m.life <= 0f || m.pos.y - m.radius > worldH + 16f || m.pos.x + m.radius < -16f || m.pos.x - m.radius > worldW + 16f) {
                m.alive = false
                missiles.removeAt(i)
                i--
                continue
            }

            i--
        }
    }

    
    private fun updateUpgradeKits(dt: Float) {
        var i = upgradeKits.size - 1
        while (i >= 0) {
            val k = upgradeKits[i]
            if (!k.alive) { upgradeKits.removeAt(i); i--; continue }
            k.pos.mulAdd(k.vel, dt)
            k.life -= dt
            
            if (k.pos.y + k.radius < -12f || k.life <= 0f) {
                k.alive = false
                upgradeKits.removeAt(i)
                i--
                continue
            }
            
            if (circlesOverlap(playerPos.x, playerPos.y, playerRadius, k.pos.x, k.pos.y, k.radius)) {
                k.alive = false
                upgradeKits.removeAt(i)
                
                val msg = applyRandomUpgrade()
                if (msg != null) {
                    upgradeMsg = "업그레이드: $msg"
                    upgradeMsgTimer = 2.5f
                }
                i--
                continue
            }
            i--
        }
    }

    private fun spawnUpgradeKit(x: Float, y: Float) {
        val k = UpgradeKit(
            pos = Vector2(x, y),
            vel = Vector2(0f, -70f),
            radius = 10f,
            alive = true,
            life = 14f
        )
        upgradeKits.add(k)
    }

    private fun updateDrones(dt: Float) {
        if (u7DroneLevel <= 0) return
        
        droneOrbitAngle += dt * (MathUtils.degreesToRadians * droneAngularVelocityDeg(u7DroneLevel))
        val fireInterval = droneFireInterval(u7DroneLevel)
        for (idx in 0 until drones.size) {
            val d = drones[idx]
            d.fireTimer -= dt
            if (d.fireTimer <= 0f) {
                d.fireTimer = fireInterval
                val ang = d.angleOffset + droneOrbitAngle
                val r = droneOrbitRadius(u7DroneLevel)
                val x = playerPos.x + MathUtils.cos(ang) * r
                val y = playerPos.y + MathUtils.sin(ang) * r
                val totalAtk = playerAtk + u9AtkBonus
                val dmg = droneDamage(totalAtk, u7DroneLevel)
                val target = nearestEnemy(x, y)
                val dir = if (target != null) {
                    val vx = target.pos.x - x
                    val vy = target.pos.y - y
                    val v = Vector2(vx, vy)
                    if (v.len2() > 0f) v.nor() else Vector2(0f, 1f)
                } else Vector2(0f, 1f)
                val spd = 780f
                val b = Bullet(
                    pos = Vector2(x, y),
                    vel = Vector2(dir.x * spd, dir.y * spd),
                    radius = 8f + 2f * u3BulletSize,
                    dmg = dmg,
                    alive = true,
                    pierce = 0,
                    fromDrone = true
                )
                bullets.add(b)
            }
        }
    }

    private fun updateLaserDrones(dt: Float) {
        if (u8LaserDroneLevel <= 0) return
        
        laserOrbitAngle += dt * 2.6f

        val d = laserDrones.getOrNull(0) ?: return

        
        val lz = u8LaserDroneLevel.coerceIn(1, 15)
        val duration = when (lz) {
            1 -> 1.0f
            2 -> 1.0f
            3 -> 1.0f
            4 -> 1.5f
            5 -> 1.5f
            6 -> 1.5f
            7 -> 1.5f
            8 -> 1.5f
            9 -> 1.5f
            10 -> 1.5f
            11 -> 1.5f
            12 -> 1.8f
            13 -> 1.8f
            14 -> 1.8f
            else -> 2.0f 
        }
        val tickIv = when {
            lz >= 15 -> 0.20f
            lz >= 8 -> 0.30f
            else -> 0.50f
        }
        val extraDmg = when (lz) {
            1 -> 0
            2 -> 2
            3 -> 5
            4 -> 5
            5 -> 7
            6 -> 10
            7 -> 12
            8 -> 15
            9 -> 20
            10 -> 25
            11 -> 30
            12 -> 30
            13 -> 30
            14 -> 40
            else -> 50 
        }
        val cd = when (lz) {
            1 -> 10.0f
            2 -> 9.5f
            3 -> 9.0f
            4 -> 9.0f
            5 -> 8.5f
            6 -> 8.0f
            7 -> 7.5f
            8 -> 7.0f
            9 -> 6.5f
            10 -> 6.5f
            11 -> 6.0f
            12 -> 6.0f
            13 -> 5.5f
            14 -> 5.0f
            else -> 5.0f 
        }

        
        if (d.beamTime <= 0f) {
            if (d.cooldown > 0f) {
                d.cooldown -= dt
            } else {
                d.beamTime = duration
                d.tickTimer = 0f
                d.tickCount = 0
            }
            return
        }

        
        val orbitR = playerRadius + 46f
        val ang = d.angleOffset + laserOrbitAngle
        val ox = playerPos.x + MathUtils.cos(ang) * orbitR
        val oy = playerPos.y + MathUtils.sin(ang) * orbitR

        
        val target = nearestEnemy(ox, oy)
        var tx = ox
        var ty = oy + 1f
        if (target != null) { tx = target.pos.x; ty = target.pos.y }
        val dirx = tx - ox
        val diry = ty - oy
        val len = kotlin.math.max(1f, kotlin.math.sqrt(dirx * dirx + diry * diry))
        val nx = dirx / len
        val ny = diry / len
        val beamLen = 1600f
        val ex = ox + nx * beamLen
        val ey = oy + ny * beamLen

        
        d.beamTime -= dt
        d.tickTimer += dt
        val beamHalfWidth = 6f
        
        run {
            val maxTicksPerFrame = 10
            var processed = 0
            while (d.tickTimer >= tickIv && processed < maxTicksPerFrame) {
                d.tickTimer -= tickIv
                d.tickCount += 1
                val dmg = (playerAtk + u9AtkBonus) + extraDmg
                for (e in enemies) if (e.alive) {
                    if (segmentIntersectsCircle(ox, oy, ex, ey, e.pos.x, e.pos.y, e.radius + beamHalfWidth)) {
                        e.hp -= dmg
                        if (e.hp <= 0) onEnemyKilled(e)
                    }
                }
                processed++
            }
        }

        
        if (d.beamTime <= 0f) {
            d.cooldown = cd
        }
    }

    private fun speedScale(): Float = when {
        stageNumber < 5 -> 0.75f
        stageNumber < 10 -> 0.9f
        else -> 1f
    }

    
    private fun atkBonusForStage(stage: Int): Int = when {
        stage < 15 -> 1
        stage < 35 -> 2
        stage < 55 -> 3
        stage < 75 -> 4
        else -> 5
    }

    private data class RectF(val x: Float, val y: Float, val w: Float, val h: Float)
    private fun pointInRect(px: Float, py: Float, r: RectF): Boolean = px >= r.x && px <= r.x + r.w && py >= r.y && py <= r.y + r.h
    private fun gameOverButtons(): Pair<RectF, RectF> {
        
        val btnW = 260f
        val btnH = 56f
        val spacing = 18f
        val x = (worldW - btnW) / 2f
        val topY = worldH * 0.44f
        val retry = RectF(x, topY, btnW, btnH)
        val menu = RectF(x, topY - (btnH + spacing), btnW, btnH)
        return retry to menu
    }
    private fun drawGameOverUI() {
        
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.58f)
        shape.rect(0f, 0f, worldW, worldH)
        
        val (retry, menu) = gameOverButtons()
        val v = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        stage.camera.unproject(v)
        val retryHover = pointInRect(v.x, v.y, retry)
        val menuHover = pointInRect(v.x, v.y, menu)
        
        shape.color = if (retryHover) Color(0.85f, 0.55f, 0.2f, 0.9f) else Color(0.7f, 0.42f, 0.16f, 0.9f)
        shape.rect(retry.x, retry.y, retry.w, retry.h)
        
        shape.color = if (menuHover) Color(0.25f, 0.45f, 0.8f, 0.9f) else Color(0.16f, 0.32f, 0.6f, 0.9f)
        shape.rect(menu.x, menu.y, menu.w, menu.h)
        shape.end()

        val batch = game.spriteBatch
        val font = game.font
        batch.begin()
        font.color = Color.WHITE
        layout.setText(font, "게임 오버")
        font.draw(batch, layout, (worldW - layout.width) / 2f, worldH * 0.64f)
        layout.setText(font, "점수: $score")
        font.draw(batch, layout, (worldW - layout.width) / 2f, worldH * 0.58f)
        layout.setText(font, "다시하기")
        font.draw(batch, layout, retry.x + (retry.w - layout.width) / 2f, retry.y + retry.h * 0.62f)
        layout.setText(font, "메인 메뉴")
        font.draw(batch, layout, menu.x + (menu.w - layout.width) / 2f, menu.y + menu.h * 0.62f)
        batch.end()
    }

    
    private fun pauseButtons(): Triple<RectF, RectF, RectF> {
        val btnW = 260f
        val btnH = 56f
        val spacing = 18f
        val x = (worldW - btnW) / 2f
        val topY = worldH * 0.48f
        val cont = RectF(x, topY, btnW, btnH)
        val retry = RectF(x, topY - (btnH + spacing), btnW, btnH)
        val menu = RectF(x, topY - 2f * (btnH + spacing), btnW, btnH)
        return Triple(cont, retry, menu)
    }
    private fun drawPauseMenuUI() {
        
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.55f)
        shape.rect(0f, 0f, worldW, worldH)
        val (cont, retry, menu) = pauseButtons()
        val v = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        stage.camera.unproject(v)
        val contHover = pointInRect(v.x, v.y, cont)
        val retryHover = pointInRect(v.x, v.y, retry)
        val menuHover = pointInRect(v.x, v.y, menu)
        
        shape.color = if (contHover) Color(0.25f, 0.7f, 0.35f, 0.9f) else Color(0.18f, 0.5f, 0.28f, 0.9f)
        shape.rect(cont.x, cont.y, cont.w, cont.h)
        
        shape.color = if (retryHover) Color(0.85f, 0.55f, 0.2f, 0.9f) else Color(0.7f, 0.42f, 0.16f, 0.9f)
        shape.rect(retry.x, retry.y, retry.w, retry.h)
        
        shape.color = if (menuHover) Color(0.25f, 0.45f, 0.8f, 0.9f) else Color(0.16f, 0.32f, 0.6f, 0.9f)
        shape.rect(menu.x, menu.y, menu.w, menu.h)
        shape.end()

        val batch = game.spriteBatch
        val font = game.font
        batch.begin()
        font.color = Color.WHITE
        layout.setText(font, "일시 정지")
        font.draw(batch, layout, (worldW - layout.width) / 2f, worldH * 0.64f)
        layout.setText(font, "계속하기")
        font.draw(batch, layout, cont.x + (cont.w - layout.width) / 2f, cont.y + cont.h * 0.62f)
        layout.setText(font, "다시하기")
        font.draw(batch, layout, retry.x + (retry.w - layout.width) / 2f, retry.y + retry.h * 0.62f)
        layout.setText(font, "메인 메뉴")
        font.draw(batch, layout, menu.x + (menu.w - layout.width) / 2f, menu.y + menu.h * 0.62f)
        batch.end()
    }
}
