package com.justxraf.skyblockevents.listeners.entities

import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.managers.LevelsManager
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.ProjectileLaunchEvent

class EntityTargetPlayerListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onEntityTarget(event: EntityTargetEvent) {
        val target = event.target
        if(target !is Player) return

        val currentEvent = eventsManager.currentEvent
        if(currentEvent.spawnLocation.world != target.world) return

        val user = target.asUser() ?: return
        // check level for entity
        val levelsManager = LevelsManager.instance
        if(!levelsManager.canKillAMob(user.level, event.entityType)) {
            event.isCancelled = false
            return
        }

        if(!currentEvent.isInSpawnRegion(target.location)) return

        event.isCancelled = true
    }
}