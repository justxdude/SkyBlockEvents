package com.justxraf.skyblockevents.listeners.portals

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.isInPortal
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import java.util.*
import kotlin.math.max
import kotlin.math.min

class PortalBreakListener : Listener {

    private val eventsManager = EventsManager.instance
    private val timeChecker: MutableMap<UUID, Long> = mutableMapOf()

    @EventHandler
    fun onPortalBlockBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val currentEvent = eventsManager.currentEvent
        if(currentEvent.portalLocation == null) return

        val portalCuboid = currentEvent.portalCuboid ?: return

        if(!isInPortal(location, portalCuboid.first, portalCuboid.second)) return
        val player = event.player

        event.isCancelled = true

        if(!shouldSendMessage(player.uniqueId)) return
        player.sendColoured("&cZostaw to")
    }
    private fun shouldSendMessage(uniqueId: UUID): Boolean {
        if(timeChecker[uniqueId] == null) {
            timeChecker[uniqueId] = System.currentTimeMillis()
            return true
        }
        if(System.currentTimeMillis() - timeChecker[uniqueId]!! > 6000) {
            timeChecker[uniqueId] = System.currentTimeMillis()
            return true
        }
        return false
    }
}