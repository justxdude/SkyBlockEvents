package com.justxraf.skyblockevents.listeners.nether.blocks

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class RegenerativeBlockListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onRegenerativeBlockBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent

    }
}