package com.justxraf.skyblockevents.listeners.points

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import kotlin.time.times

class EntityKillListener : Listener {
    val eventsManager = EventsManager.instance

    @EventHandler
    fun onEntityKill(event: EntityDeathEvent) {
        val entity = event.entity
        val currentEvent = eventsManager.currentEvent
        if(entity.world != currentEvent.spawnLocation.world) return

        val killer = entity.killer ?: return
        currentEvent.pointsHandler.addPoints(killer.uniqueId, getReward(entity.type, 1.0))
    }
    // TODO add mythic mobs support with levels
    private fun getReward(entityType: EntityType, level: Double): Int = when (entityType) {
        EntityType.BLAZE -> (3 * (level * 0.3)).toInt()
        else -> 1
    }
}