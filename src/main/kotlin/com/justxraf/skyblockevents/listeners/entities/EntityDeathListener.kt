package com.justxraf.skyblockevents.listeners.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class EntityDeathListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val currentEvent = eventsManager.currentEvent
        val world = currentEvent.spawnLocation.world

        val entity = event.entity
        val entityWorld = entity.world

        if(world != entityWorld) return // Need the same world as the event

        val killer = entity.killer
        if(killer !is Player) return

        // delete the entity from event
        currentEvent.removeEntity(entity.uniqueId)
    }
    @EventHandler
    fun onEntityMove(event: EntityMoveEvent) {
        if(event.entity.location.y > .0) return

        val currentEvent = eventsManager.currentEvent
        val world = currentEvent.spawnLocation.world

        val entity = event.entity
        val entityWorld = entity.world

        if(world != entityWorld) return

        Bukkit.getPluginManager().callEvent(EntityDeathEvent(entity, DamageSource.builder(DamageType.FALL).build(), mutableListOf()))
        entity.remove()
    }
}