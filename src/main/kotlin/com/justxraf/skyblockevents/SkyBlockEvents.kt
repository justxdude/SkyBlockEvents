package com.justxraf.skyblockevents

import com.justxraf.skyblockevents.commands.EventCommand
import com.justxraf.skyblockevents.components.ComponentsManager
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.plugin.java.JavaPlugin


class SkyBlockEvents : JavaPlugin() {
    override fun onEnable() {
        try {
            var world = Bukkit.getWorld("world_nether_event")
            if (world == null) {
                world = Bukkit.createWorld(WorldCreator("world_nether_event").environment(World.Environment.NETHER))
                requireNotNull(world) { "Failed to load or create world 'world_nether_event'" }
            }

            instance = this
            ComponentsManager.initialize(this)
            dataFolder.mkdirs()

            EventCommand()
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onDisable() {

    }
    companion object {
        lateinit var instance: SkyBlockEvents
    }
}