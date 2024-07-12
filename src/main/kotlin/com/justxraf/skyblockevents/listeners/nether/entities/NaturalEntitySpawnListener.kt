package com.justxraf.skyblockevents.listeners.nether.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import org.bukkit.entity.SpawnCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent

class NaturalEntitySpawnListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val currentEvent = eventsManager.currentEvent
        if (currentEvent !is NetherEvent) return

        val entity = event.entity
        val world = currentEvent.world

        val entityWorld = entity.world.name
        if(world != entityWorld) return

        event.isCancelled = true
    }
}