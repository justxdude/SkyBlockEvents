package com.justxraf.skyblockevents.listeners.nether.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.listeners.ListenersManager
import org.bukkit.entity.Animals
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.SpawnCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntitySpawnEvent

class NaturalEntitySpawnListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance
    @EventHandler
    fun onEntitySpawn(event: CreatureSpawnEvent) {
        val currentEvent = eventsManager.currentEvent

        if (currentEvent !is NetherEvent) return
        val entity = event.entity

        val location = entity.location
        if(!listenersManager.doChecks(location, currentEvent.spawnLocation)) return

        if(event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) return
        if(currentEvent.isEventEntity(entity.uniqueId)) return

        event.isCancelled = true
    }
}