package com.justxraf.skyblockevents.listeners.players

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if(!player.hasPermission("hyperiol.events.admin")) return

        eventsManager.editSession.remove(player.uniqueId)
    }
}