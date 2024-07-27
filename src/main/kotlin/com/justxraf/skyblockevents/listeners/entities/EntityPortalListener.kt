package com.justxraf.skyblockevents.listeners.entities

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPortalEvent

class EntityPortalListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onEntityPortal(event: EntityPortalEvent) {
        val entity = event.entity
        if(entity is Player) return

        val location = entity.location
        val eventData = eventsManager.events.firstNotNullOfOrNull { it.value.spawnLocation.world == location.world }

        if(eventData == null) return
        event.isCancelled = true
    }
}