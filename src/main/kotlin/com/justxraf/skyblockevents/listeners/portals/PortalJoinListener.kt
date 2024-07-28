package com.justxraf.skyblockevents.listeners.portals

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.isInCuboid
import com.justxraf.skyblockevents.util.pushIfClose
import com.justxraf.skyblockevents.util.shouldSendMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*

class PortalJoinListener : Listener {
    private val eventsManager = EventsManager.instance
    private val timeChecker: MutableMap<UUID, Long> = mutableMapOf()
    @EventHandler
    fun onPortalJoin(event: PlayerMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        val portalLocation = currentEvent.portalLocation ?: return

        val player = event.player
        if(player.world != portalLocation.world) return

        val portalCuboid = currentEvent.portalCuboid ?: return
        if(!player.location.isInCuboid(portalCuboid.first.clone().subtract(10.0, .0, 10.0), portalCuboid.second.clone().add(10.0, .0, 10.0))) return

        val materialInLocation = player.world.getBlockAt(player.location.add(.3, .5,.3)).type
        if(materialInLocation != Material.NETHER_PORTAL) return

        if(Bukkit.getOnlinePlayers().isEmpty()) { // TODO do zmiany po testach na 5 (i więcej z czasem)
            player.pushIfClose(player.location.clone().add(.2, 1.0, .2), .3, 1.2)
            if(!timeChecker.shouldSendMessage(player.uniqueId)) return

            player.sendColoured("&cNie ma wystarczająco graczy na serwerze, abyś mógł/a dołączyć do wydarzenia!")
            return
        }
        val skyblockUser = player.asUser() ?: return
        if(skyblockUser.level < currentEvent.requiredLevel) {
            player.pushIfClose(player.location.clone().subtract(0.2, .0, 0.2), 3.5, 1.2)

            if(!timeChecker.shouldSendMessage(player.uniqueId)) return
            player.sendColoured("&cMusisz osiągnąć ${currentEvent.requiredLevel} poziom aby móc dołączyć do tego wydarzenia!")
            return
        }
        currentEvent.teleport(player)
    }
}