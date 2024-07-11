package com.justxraf.skyblockevents

import com.justxraf.skyblockevents.commands.EventCommand
import com.justxraf.skyblockevents.components.ComponentsManager
import org.bukkit.plugin.java.JavaPlugin

class SkyBlockEvents : JavaPlugin() {
    override fun onEnable() {
        ComponentsManager.initialize(this)
        dataFolder.mkdirs()

        EventCommand()
    }
    override fun onDisable() {

    }
}