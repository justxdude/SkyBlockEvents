package com.justxraf.skyblockevents.listeners.portals

import com.justxraf.networkapi.util.sendColoured
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

    @EventHandler
    fun onPortalDamage(event: BlockBreakEvent) {
        val block = event.block
        val location = block.location

        val currentEvent = eventsManager.currentEvent
        if(currentEvent.getPortalAt(location) == null) return

        event.isCancelled = true
    }

}