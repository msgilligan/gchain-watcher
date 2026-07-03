plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

javafx {
    version = "25.0.3"
    modules("javafx.base")
}

dependencies {
    implementation("org.java-gi:gtk:1.0.0-RC1")
    //implementation("org.java-gi:adw:1.0.0-RC1")  // libadwaita (doesn't work will on macOS)
    implementation("org.bitcoinj:bitcoinj-core:0.17.1")
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("org.slf4j:slf4j-jdk14:2.0.17")
}

val isMacOS = System.getProperty("os.name").lowercase().contains("mac")

tasks.named<JavaExec>("run") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")

    if (isMacOS) {
        jvmArgs("-XstartOnFirstThread")
    }
}

val installDir = layout.buildDirectory.dir("install/${project.name}")

val nativeLibs: String? =
    System.getenv("DYLD_LIBRARY_PATH") ?: System.getenv("LD_LIBRARY_PATH")

tasks.register<Exec>("trainAotCache") {
    dependsOn("installDist")
    commandLine(installDir.get().file("bin/${project.name}").asFile.absolutePath)
    environment("CHAINWATCHER_AOT_TRAINING", "1")
    environment("JAVA_OPTS", buildString {
        append("-XX:AOTCacheOutput=${installDir.get().file("lib/app.aot").asFile.absolutePath}")
        if (nativeLibs != null) append(" -Djava.library.path=$nativeLibs")
    })
}

application {
    mainClass = "ChainWatcher"
    applicationDefaultJvmArgs = buildList {
        add("--enable-native-access=ALL-UNNAMED")
        //add("-Xlog:aot")
        nativeLibs?.let { add("-Djava.library.path=$it") }
        if (isMacOS) add("-XstartOnFirstThread")
    }
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        val aotBlock = """
            |
            |# Use the AOT cache if present, unless this is a training run
            |if [ -z "${'$'}CHAINWATCHER_AOT_TRAINING" ] && [ -f "${'$'}APP_HOME/lib/app.aot" ] ; then
            |    DEFAULT_JVM_OPTS="${'$'}DEFAULT_JVM_OPTS \"-XX:AOTCache=${'$'}APP_HOME/lib/app.aot\""
            |fi""".trimMargin()

        unixScript.writeText(unixScript.readText().replace(
            Regex("(?m)^DEFAULT_JVM_OPTS=.*$")
        ) { it.value + aotBlock })
    }
}
