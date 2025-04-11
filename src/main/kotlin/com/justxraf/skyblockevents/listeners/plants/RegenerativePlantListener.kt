package com.justxraf.skyblockevents.listeners.plants

import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.eventsTranslation
import com.justxraf.skyblockevents.util.shouldSendMessage
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

class RegenerativePlantListener : Listener {
    private val eventsManager = EventsManager.instance
    private val timer: MutableMap<UUID, Long> = mutableMapOf()

    @EventHandler
    fun onPlantBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent

        val block = event.block
        if(block.location.world != currentEvent.spawnLocation.world) return

        val player = event.player
        val location = block.location


        if (!currentEvent.regenerativeMaterialsManager.isRegenerative(block.type)) return

        if(!currentEvent.regenerativeMaterialsManager.canBreakMaterialAt(location, block.type)) {
            if(timer.shouldSendMessage(player.uniqueId)) player.sendColoured("let.it.grow".eventsTranslation(player))
            event.isCancelled = true
            return
        }
        currentEvent.regenerativeMaterialsManager.breakRegenerativeMaterial(location, block.type)
    }
}