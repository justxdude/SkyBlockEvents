import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.21"
    application
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
    maven {
        name = "citizensRepo"
        url = uri("https://maven.citizensnpcs.co/repo")
    }
    maven { url = uri("https://jitpack.io") }

}

dependencies {
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.6")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly(files("lib/SkyblockAPI.jar"))
    compileOnly(files("lib/NetworkAPI.jar"))
    compileOnly(files("lib/QuestsCore.jar"))
    compileOnly("net.citizensnpcs:citizens-main:2.0.33-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation(platform("com.intellectualsites.bom:bom-newest:1.41"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}