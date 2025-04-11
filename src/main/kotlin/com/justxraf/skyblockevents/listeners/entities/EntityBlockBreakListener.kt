package com.justxraf.skyblockevents.listeners.entities

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent

class EntityBlockBreakListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onBlockBreak(event: EntityExplodeEvent) {
        val currentEvent = eventsManager.currentEvent

        val location = event.entity.location
        if(location.world != currentEvent.spawnLocation.world) return

        event.blockList().clear()
    }
}