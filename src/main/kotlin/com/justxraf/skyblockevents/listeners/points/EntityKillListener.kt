package com.justxraf.skyblockevents.listeners.points

import com.justxraf.skyblockevents.events.EventsManager
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import kotlin.jvm.optionals.getOrNull
import kotlin.time.times

class EntityKillListener : Listener {
    val eventsManager = EventsManager.instance

    @EventHandler
    fun onEntityKill(event: EntityDeathEvent) {
        val entity = event.entity
        val currentEvent = eventsManager.currentEvent
        if(entity.world != currentEvent.spawnLocation.world) return

        val killer = entity.killer ?: return
        val activeMob = MythicBukkit.inst().mobManager.getActiveMob(entity.uniqueId).getOrNull()

        if(activeMob == null) return

        val eventUser = currentEvent.eventUserHandler.getUser(killer.uniqueId) ?: return
        eventUser.addPoints(getReward(entity.type, activeMob.level))
        eventUser.mobsKilled += 1
    }

    private fun getReward(entityType: EntityType, level: Double): Int = when (entityType) {
        EntityType.BLAZE -> (3 * (level * 1.3)).toInt()
        else -> 1
    }
}