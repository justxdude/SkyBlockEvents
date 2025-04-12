import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.0.21"
}


group = "com.justxraf"
version = "1.0-SNAPSHOT"
tasks.jar {
    archiveFileName.set("SkyBlockEvents.jar")
    destinationDirectory.set(file("C:/Users/justxdude/OneDrive/Desktop/Servers/skyblock/plugins"))
}
repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }
    maven {
        url = uri(("https://repo.papermc.io/repository/maven-public/"))
    }
    maven { url = uri("https://jitpack.io") }

    maven("https://repo.fancyplugins.de/releases")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.lumine:Mythic-Dist:5.8.1")

    compileOnly("de.oliver:FancyNpcs:2.4.4")
    compileOnly("de.oliver:FancyHolograms:2.4.2")

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    compileOnly(files("lib/SkyblockAPI.jar"))
    compileOnly(files("lib/IslandCore.jar"))
    compileOnly(files("lib/NetworkAPI.jar"))
    compileOnly(files("lib/QuestsCore.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation(platform("com.intellectualsites.bom:bom-newest:1.41"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}