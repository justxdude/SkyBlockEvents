package com.justxraf.skyblockevents.listeners.players

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkUnloadEvent

class PlayerQuitListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if(!player.hasPermission("hyperiol.events.admin")) return

        eventsManager.editSession.remove(player.uniqueId)
    }
    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        val currentEvent = eventsManager.currentEvent

        if(!currentEvent.activePlayers.contains(player.uniqueId)) return

        currentEvent.activePlayers.remove(player.uniqueId)
    }
    @EventHandler
    fun dwa(event: ChunkUnloadEvent) {
        val currentEvent = eventsManager.currentEvent
        if(event.world != currentEvent.spawnLocation.world)  return

        event.chunk.isForceLoaded = true
    }
}