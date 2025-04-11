package com.justxraf.skyblockevents.listeners.portals

import com.justxdude.skyblockapi.SkyblockAPI
import com.justxraf.networkapi.util.sendColoured
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.portals.EventPortalType
import com.justxraf.skyblockevents.util.eventsTranslation
import com.justxraf.skyblockevents.util.pushIfClose
import com.justxraf.skyblockevents.util.shouldSendMessage
import de.oliver.fancynpcs.api.events.NpcRemoveEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*

class PortalJoinListener : Listener {

    private val eventsManager = EventsManager.instance
    private val timeChecker: MutableMap<UUID, Long> = mutableMapOf()

    @EventHandler
    fun onPlayerPortalJoin(event: PlayerMoveEvent) {
        val player = event.player
        val location = event.to ?: return

        val materialInLocation = location.world?.getBlockAt(player.location.add(.3, .5,.3))?.type ?: return
        if(materialInLocation != Material.NETHER_PORTAL) return

        val portal = eventsManager.currentEvent.getPortalAt(location) ?: return
        when(portal.portalType) {
            EventPortalType.EVENT -> SkyblockAPI.instance.spawn.teleport(player, true)
            else -> {
                if(Bukkit.getOnlinePlayers().isEmpty()) { // TODO do zmiany po testach na 5 (i więcej z czasem)
                    player.pushIfClose(player.location.clone().add(.2, 1.0, .2), .3, 1.2)
                    if(!timeChecker.shouldSendMessage(player.uniqueId)) return

                    player.sendColoured("not.enough.players".eventsTranslation(player))
                    return
                }
                val skyBlockUser = player.asUser() ?: return
                if(skyBlockUser.level < eventsManager.currentEvent.requiredLevel) {
                    player.pushIfClose(player.location.clone().subtract(0.2, .0, 0.2), 3.5, 1.2)

                    if(!timeChecker.shouldSendMessage(player.uniqueId)) return
                    player.sendColoured("not.enough.level".eventsTranslation(player, eventsManager.currentEvent.requiredLevel.toString()))
                    return
                }
                eventsManager.currentEvent.teleport(player)
            }
        }
    }
}