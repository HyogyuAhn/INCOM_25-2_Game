plugins {
    application
}

dependencies {
    implementation(project(":core"))

    val gdxVersion = "1.13.5"
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.club.desktop.DesktopLauncherKt")
}

tasks.named<JavaExec>("run") {
    // macOS에서 LWJGL3는 첫 스레드 필요
    jvmArgs = listOf("-XstartOnFirstThread")
    // 에셋 디렉터리를 워킹 디렉터리로 설정
    workingDir = project.rootProject.layout.projectDirectory.dir("assets").asFile
    systemProperty("assets.path", workingDir.absolutePath)
}

