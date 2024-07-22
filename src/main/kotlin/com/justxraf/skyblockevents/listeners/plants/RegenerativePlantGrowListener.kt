package com.justxraf.skyblockevents.listeners.plants

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockGrowEvent

class RegenerativePlantGrowListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onPlantGrow(event: BlockGrowEvent) {
        val currentEvent = eventsManager.currentEvent
        val block = event.block

        if(block.location.world != currentEvent.spawnLocation.world) return
        if(!currentEvent.isRegenerativePlant(block.location)) return

        if(currentEvent.activePlayers.isEmpty()) {
            event.isCancelled = true
            return
        }
    }
}