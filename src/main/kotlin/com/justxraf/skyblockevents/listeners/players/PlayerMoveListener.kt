package com.justxraf.skyblockevents.listeners.players

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

class PlayerMoveListener : Listener {
    private val eventsManager = EventsManager.instance
    private val cache: MutableMap<UUID, Long> = mutableMapOf()
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val now = System.currentTimeMillis()
        val lastCache = cache[player.uniqueId] ?: 0L

        if (lastCache + (60 * 1000) > now) {
            return
        } else {
            val currentEvent = eventsManager.currentEvent
            if(!currentEvent.activePlayers.contains(player.uniqueId)) return

            currentEvent.activePlayers[player.uniqueId]?.lastCache = System.currentTimeMillis()

            cache[player.uniqueId] = now // Update the cache time after checking
        }
    }
}