package com.justxraf.skyblockevents.commands.admin

import com.justxdude.islandcore.utils.toLocationString
import com.justxraf.networkapi.util.LocationUtil.isItSafe
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.portals.EventPortalType
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.events.portals.EventPortal
import com.justxraf.skyblockevents.util.getLookingDirection
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

object EventAdminSetPortalSubCommand {

    /*

    /event setportal nether/normal <x> <y> <z>

     */
    private fun shouldProcess(player: Player, sessionEvent: EventData, args: Array<String>): EventPortalType? {
        if(args.size < 2) {
            player.sendColoured("&cNiepoprawne użycie! Użyj /event setportal event/normal (nic lub X, Y, Z).")
            return null
        }
        val type = EventPortalType.entries.firstOrNull { it.name == args[1].uppercase() }
        if(type == null) {
            player.sendColoured("&cNiepoprawny typ portalu! Użyj /event setportal event/normal (nic lub X, Y, Z).")
            return null
        }
        if(args.size > 2 && args.size != 5) {
            player.sendColoured("&cNiepoprawne użycie! Użyj /event setportal event/normal X Y Z.")
            return null
        }
        val location = getLocationFrom(player, args)

        if(!location.isItSafe()) {
            player.sendColoured("&cTa lokacja nie jest bezpieczna, aby postawić portal. Spróbuj innego miejsca.")
            return null
        }

        when(type) {
            EventPortalType.EVENT -> {
                if(location.world != sessionEvent.spawnLocation.world) {
                    player.sendColoured("&cPortal do spawnu musi być ustawiony w świecie wydarzenia.")
                    return null
                }
            }
            EventPortalType.NORMAL -> {
                if(location.world == sessionEvent.spawnLocation.world) {
                    player.sendColoured("&cNie możesz ustawić portalu do wydarzenia w tym samym świecie, w którym jest wydarzenie.")
                    return null
                }
            }
        }
        return type
    }
    fun processSetPortal(player: Player, args: Array<String>,sessionEvent: EventData, currentEvent: Event?) {
        val type = shouldProcess(player, sessionEvent, args) ?: return
        val location = getLocationFrom(player, args)

        player.sendColoured("&7Ustawiono portal dla wydarzenia #${sessionEvent.uniqueId} w lokacji (XYZ) ${location.toLocationString()}")

        val portal = EventPortal(type, player.getLookingDirection(), location, sessionEvent.type)

        if (sessionEvent.portals.isNullOrEmpty()) sessionEvent.portals = ConcurrentHashMap()
        sessionEvent.portals?.set(type, portal)

        if (currentEvent == null) return
        if (currentEvent.portals.isNullOrEmpty()) currentEvent.portals = ConcurrentHashMap()

        currentEvent.portals?.set(type, portal)
        portal.setup()
    }
    private fun getLocationFrom(player: Player, args: Array<String>): Location = if(args.size == 5)
        Location(player.location.world, args[2].toDouble(), args[3].toDouble(), args[4].toDouble()) else player.location
}