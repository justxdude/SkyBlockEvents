package com.justxraf.skyblockevents.listeners.portals

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.isInCuboid
import com.justxraf.skyblockevents.util.shouldSendMessage
import org.bukkit.Location
import org.bukkit.entity.Player
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

        val portalLocation = currentEvent.portalLocation ?: return
        if(portalLocation.world != location.world) return

        val portalCuboid = currentEvent.portalCuboid ?: return

        if(!processBlockBreakEvent(location, portalCuboid)) return
        event.isCancelled = true
    }
    @EventHandler
    fun onEventPortalBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val currentEvent = eventsManager.currentEvent

        val player = event.player
        if(player.world != currentEvent.spawnLocation.world) return

        if(currentEvent.eventPortalLocation == null) return
        val eventPortalCuboid = currentEvent.eventPortalCuboid ?: return

        if(!processBlockBreakEvent(location, eventPortalCuboid)) return
        event.isCancelled = true
    }
    private fun processBlockBreakEvent(location: Location, cuboid: Pair<Location, Location>): Boolean {
        val (loc1, loc2) = cuboid
        return isInCuboid(location, loc1, loc2)
    }

}