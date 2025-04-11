package com.justxraf.skyblockevents.listeners.blocks

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class RegenerativeBlockListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent

        val block = event.block
        val location = block.location

        if (!listenersManager.doChecks(location, currentEvent.spawnLocation)) return

        if (!currentEvent.regenerativeMaterialsManager.isRegenerative(block.type)
            && !event.player.hasPermission("hyperiol.events.admin")) {
            event.isCancelled = true
            return
        }

        currentEvent.regenerativeMaterialsManager.breakRegenerativeMaterial(location, block.type)
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val currentEvent = eventsManager.currentEvent

        val block = event.block
        val location = block.location

        if(!listenersManager.doChecks(location, currentEvent.spawnLocation)) return

        if(!event.player.hasPermission("hyperiol.events.admin")) {
            event.isCancelled = true
            return
        }
    }
}