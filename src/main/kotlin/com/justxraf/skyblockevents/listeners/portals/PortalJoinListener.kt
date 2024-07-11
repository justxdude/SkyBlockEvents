package com.justxraf.skyblockevents.listeners.portals

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.isInPortal
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PortalJoinListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onPortalJoin(event: PlayerMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        val portalLocation = currentEvent.portalLocation ?: return

        val player = event.player
        if(player.world != portalLocation.world) return

        val portalCuboid = currentEvent.portalCuboid ?: return
        if(!isInPortal(player.location, portalCuboid.first, portalCuboid.second)) return

        val materialInLocation = player.world.getBlockAt(player.location.add(.0, .5,.0)).type
        if(materialInLocation != Material.NETHER_PORTAL) return

        if(Bukkit.getOnlinePlayers().size < 1) { // TODO do zmiany po testach na 5 (i więcej z czasem)
            player.sendColoured("&cNie ma wystarczająco graczy na serwerze, abyś mógł/mogła dołączyć do wydarzenia!")
            return
        }

        currentEvent.teleport(player)

    }
}