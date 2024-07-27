package com.justxraf.skyblockevents.listeners

import com.github.supergluelib.foundation.registerListeners
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.npcs.QuestNPCInteractListener
import com.justxraf.skyblockevents.listeners.blocks.RegenerativeBlockListener
import com.justxraf.skyblockevents.listeners.plants.RegenerativePlantListener
import com.justxraf.skyblockevents.listeners.entities.EntityDamageListener
import com.justxraf.skyblockevents.listeners.entities.EntityDeathListener
import com.justxraf.skyblockevents.listeners.entities.EntityPortalListener
import com.justxraf.skyblockevents.listeners.entities.NaturalEntitySpawnListener
import com.justxraf.skyblockevents.listeners.npcs.QuestNpcPlayerNearbyListener
import com.justxraf.skyblockevents.listeners.plants.RegenerativePlantGrowListener
import com.justxraf.skyblockevents.listeners.players.PlayerDeathListener
import com.justxraf.skyblockevents.listeners.players.PlayerJoinListener
import com.justxraf.skyblockevents.listeners.players.PlayerQuitListener
import com.justxraf.skyblockevents.listeners.portals.SkyBlockEventQuitListener
import com.justxraf.skyblockevents.listeners.portals.PortalBreakListener
import com.justxraf.skyblockevents.listeners.portals.PortalJoinListener
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class ListenersManager(private val plugin: JavaPlugin) {
    fun doChecks(entityLocation: Location, eventLocation: Location): Boolean = entityLocation.world == eventLocation.world

    private fun setup() {
        // Blocks
        plugin.registerListeners(
            RegenerativeBlockListener()
        )
        // Entities
        plugin.registerListeners(
            EntityDamageListener(),
            EntityDeathListener(),
            NaturalEntitySpawnListener(),
        )
        // NPCs
        plugin.registerListeners(
            QuestNPCInteractListener(),
            QuestNpcPlayerNearbyListener(),
            EntityPortalListener(),

        )
        // Plants
        plugin.registerListeners(
            RegenerativePlantListener(),
            RegenerativePlantGrowListener(),
        )
        // Players
        plugin.registerListeners(
            PlayerDeathListener(),
            PlayerJoinListener(),
            PlayerQuitListener()
        )
        plugin.registerListeners(
            PortalBreakListener(),
            PortalJoinListener(),
            SkyBlockEventQuitListener(),
        )
    }
    companion object {
        lateinit var instance: ListenersManager
        fun initialize(plugin: SkyBlockEvents): ListenersManager {
            instance = ListenersManager(plugin)
            instance.setup()
            return instance
        }
    }
}