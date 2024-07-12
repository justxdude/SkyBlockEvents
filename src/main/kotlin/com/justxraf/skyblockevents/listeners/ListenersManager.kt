package com.justxraf.skyblockevents.listeners

import com.github.supergluelib.foundation.registerListeners
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.listeners.nether.QuestNPCInteractListener
import com.justxraf.skyblockevents.listeners.nether.entities.EntityDamageListener
import com.justxraf.skyblockevents.listeners.nether.entities.EntityDeathListener
import com.justxraf.skyblockevents.listeners.portals.PortalBreakListener
import com.justxraf.skyblockevents.listeners.portals.PortalJoinListener
import org.bukkit.plugin.java.JavaPlugin

class ListenersManager(private val plugin: JavaPlugin) {
    private fun setup() {
        plugin.registerListeners(
            // Portals
            PortalBreakListener(),
            PortalJoinListener(),


            PlayerJoinListener(),
        )

        // Nether Event
        plugin.registerListeners(
            QuestNPCInteractListener(),

            // entity
            EntityDamageListener(),
            EntityDeathListener(),

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