package com.justxraf.skyblockevents.listeners

import com.github.supergluelib.foundation.registerListeners
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.nether.QuestNPCInteractListener
import com.justxraf.skyblockevents.listeners.nether.blocks.RegenerativeBlockListener
import com.justxraf.skyblockevents.listeners.nether.entities.EntityDamageListener
import com.justxraf.skyblockevents.listeners.nether.entities.EntityDeathListener
import com.justxraf.skyblockevents.listeners.nether.entities.NaturalEntitySpawnListener
import com.justxraf.skyblockevents.listeners.nether.players.PlayerDeathListener
import com.justxraf.skyblockevents.listeners.portals.SkyBlockEventQuitListener
import com.justxraf.skyblockevents.listeners.portals.PortalBreakListener
import com.justxraf.skyblockevents.listeners.portals.PortalJoinListener
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class ListenersManager(private val plugin: JavaPlugin) {
    private val eventsManager = EventsManager.instance
    fun doChecks(entityLocation: Location, eventLocation: Location): Boolean = entityLocation.world == eventLocation.world

    private fun setup() {
        plugin.registerListeners(
            // Portals
            PortalBreakListener(),
            PortalJoinListener(),

            PlayerJoinListener(),
            QuestNpcPlayerNearbyListener(),
        )

        // Nether Event
        plugin.registerListeners(
            QuestNPCInteractListener(),

            // entity
            EntityDamageListener(),
            EntityDeathListener(),
            NaturalEntitySpawnListener(),
            RegenerativeBlockListener(),

            // player
            PlayerDeathListener(),
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