package com.justxraf.skyblockevents.listeners.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntitySpawnEvent

class NaturalEntitySpawnListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onEntitySpawn(event: CreatureSpawnEvent) {
        val entity = event.entity

        val eventData = eventsManager
            .events
            .firstNotNullOfOrNull { if(it.value.spawnLocation.world == entity.world) it.value else null }
            ?: return

        if(event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) return
        event.isCancelled = true
    }
}