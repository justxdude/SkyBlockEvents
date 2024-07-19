package com.justxraf.skyblockevents.listeners

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.pushPlayerIfClose
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

        pushPlayerIfClose(questNpcLocation, event.player, 1.3, .15)
    }
}