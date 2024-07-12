package com.justxraf.skyblockevents.listeners.nether.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class EntityDeathListener : Listener {
    // TODO Checks: On entity death (remove from the map etc.), on entity void fall, on entity damage another entity
    // TODO Checks: Add a check to control player death (there is an event in SkyblockAPI which controls it...)
    // TODO Checks: Do the void fall (if y < -100 or sth)
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val currentEvent = eventsManager.currentEvent
        if(currentEvent !is NetherEvent) return

        val world = currentEvent.world

        val entity = event.entity
        val entityWorld = entity.world.name

        if(world != entityWorld) return // Need the same world as the event

        val killer = entity.killer
        if(killer !is Player) return

        // delete the entity from event
        currentEvent.removeEntity(entity.uniqueId)
        println("Debug: Removed an entity from event ${currentEvent.name}, id ${currentEvent.uniqueId}") // TODO remove
    }
}