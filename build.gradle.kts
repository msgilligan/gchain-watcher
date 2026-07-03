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

application {
    mainClass = "ChainWatcher"
    if (isMacOS) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}
