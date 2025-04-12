package com.justxraf.skyblockevents

import com.justxraf.skyblockevents.commands.admin.EventAdminCommand
import com.justxraf.skyblockevents.commands.players.EventInfoCommand
import com.justxraf.skyblockevents.components.ComponentsManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.plugin.java.JavaPlugin


class SkyBlockEvents : JavaPlugin() {
    override fun onEnable() {
        try {
            createWorlds()
            instance = this

            ComponentsManager.initialize(this)
            dataFolder.mkdirs()

            EventAdminCommand()
            EventInfoCommand()

        }catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun createWorlds() {
        var world = Bukkit.getWorld("world_nether_event")
        if (world == null) {
            world = Bukkit.createWorld(WorldCreator("world_nether_event").environment(World.Environment.NETHER))
            requireNotNull(world) { "Failed to load or create world 'world_nether_event'" }
        }
    }
    override fun onDisable() {

    }
    companion object {
        lateinit var instance: SkyBlockEvents
    }
}