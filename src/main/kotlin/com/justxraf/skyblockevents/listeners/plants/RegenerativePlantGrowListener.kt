package com.justxraf.skyblockevents.listeners.plants

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockGrowEvent

class RegenerativePlantGrowListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onPlantGrow(event: BlockGrowEvent) {
        val currentEvent = eventsManager.currentEvent
        val block = event.block

        if (block.location.world != currentEvent.spawnLocation.world) return
        if (!currentEvent.regenerativeMaterialsHandler.isRegenerative(block.type)) return

        val ageable = block.blockData as? Ageable
        if (currentEvent.activePlayers.isEmpty()) {
            event.isCancelled = true
        } else {
            if (ageable != null) {
                if (ageable.age < ageable.maximumAge) {
                    ageable.age += 1
                    block.blockData = ageable
                    block.state.update(true)
                }
            }
        }
    }

}