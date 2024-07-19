package com.justxraf.skyblockevents.listeners.portals

import com.justxdude.skyblockapi.SkyblockAPI
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.listeners.ListenersManager
import com.justxraf.skyblockevents.util.isInCuboid
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class SkyBlockEventQuitListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance

    // TODO Later on when there is more events - Portals should work for every event as there will always be a "return portal".
    @EventHandler
    fun onPortalTeleport(event: PlayerMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        val player = event.player

        val playerLocation = player.location
        val currentEventLocation = currentEvent.spawnLocation

        if(!listenersManager.doChecks(playerLocation, currentEventLocation)) return

        val eventPortalCuboid = currentEvent.eventPortalCuboid ?: return
        if(!isInCuboid(playerLocation, eventPortalCuboid.first, eventPortalCuboid.second)) return

        val materialInLocation = playerLocation.world?.getBlockAt(player.location.add(.0, .5,.0))?.type
        if(materialInLocation != null && materialInLocation != Material.NETHER_PORTAL) return

        SkyblockAPI.instance.spawn.teleport(player)
    }
}