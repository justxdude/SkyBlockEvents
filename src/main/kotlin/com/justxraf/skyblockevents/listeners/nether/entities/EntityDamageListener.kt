package com.justxraf.skyblockevents.listeners.nether.entities

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.listeners.ListenersManager
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent

class EntityDamageListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance

    // Cancel the event if a monster shoots a projectile and the hitEntity is a monster,
    // So they won't deal damage to each other.
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val currentEvent = eventsManager.currentEvent
        if(currentEvent !is NetherEvent) return

        val projectile = event.entity
        if(!listenersManager.doChecks(event.entity.location, currentEvent.spawnLocation)) return

        if(projectile.shooter is Player) return

        val hitEntity = event.hitEntity
        if(hitEntity !is Monster) return

        event.isCancelled = true
    }
}