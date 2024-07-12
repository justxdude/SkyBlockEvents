package com.justxraf.skyblockevents.listeners.nether.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent

class EntityDamageListener : Listener {
    private val eventsManager = EventsManager.instance

    // Cancel the event if a projectile is shot by a monster and the hitEntity is a monster
    // So they won't deal damage to each other.
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val currentEvent = eventsManager.currentEvent
        if(currentEvent !is NetherEvent) return

        val world = currentEvent.world
        val projectile = event.entity

        if(world != projectile.world.name) return
        if(projectile.shooter is Player) return

        val hitEntity = event.hitEntity
        if(hitEntity !is Monster) return

        event.isCancelled = true
    }
    @EventHandler
    private fun onEntityVoidFall(event: EntityMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        if(currentEvent !is NetherEvent) return

        val world = currentEvent.world
        val entity = event.entity
        val entityLocation = entity.location

        val entityWorld = entityLocation.world.name
        if(world != entityWorld) return

        if(entityLocation.y > 0) return
        entity.remove()

        Bukkit.getPluginManager().callEvent(EntityDeathEvent(event.entity, mutableListOf()))
    }
}