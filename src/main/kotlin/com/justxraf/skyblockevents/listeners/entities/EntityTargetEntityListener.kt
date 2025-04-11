package com.justxraf.skyblockevents.listeners.entities

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent

class EntityTargetEntityListener : Listener {
    private val eventsManager = EventsManager.instance

    // Prevents Piglins from attacking wither skeletons...
    @EventHandler
    fun onEntityTargetEntity(event: EntityTargetLivingEntityEvent) {
        val entity = event.entity
        val target = event.target

        if(target is Player) return

        val currentEvent = eventsManager.currentEvent
        if(currentEvent.spawnLocation.world == entity.world) event.isCancelled = true
    }
}