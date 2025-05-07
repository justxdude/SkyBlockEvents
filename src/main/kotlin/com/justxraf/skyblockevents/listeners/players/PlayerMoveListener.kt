package com.justxraf.skyblockevents.listeners.players

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerMoveListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        val eventUserHandler = currentEvent.eventUserHandler

        if(currentEvent.spawnLocation.world != event.player.world) return

        val eventUser = eventUserHandler.getUser(event.player.uniqueId)
        eventUser?.lastCache = System.currentTimeMillis()
    }
}