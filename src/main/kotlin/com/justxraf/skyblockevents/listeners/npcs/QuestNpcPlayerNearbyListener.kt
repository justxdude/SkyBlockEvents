package com.justxraf.skyblockevents.listeners.npcs

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.pushIfClose
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class QuestNpcPlayerNearbyListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        val questNpcLocation = currentEvent.questNPCLocation ?: return

        if(questNpcLocation.world != event.player.world) return

        event.player.pushIfClose(questNpcLocation, 1.3, .15)
    }
}