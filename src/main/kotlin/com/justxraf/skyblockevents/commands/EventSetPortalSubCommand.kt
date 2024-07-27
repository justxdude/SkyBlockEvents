package com.justxraf.skyblockevents.commands

import com.justxdude.islandcore.utils.toLocationString
import com.justxraf.networkapi.util.LocationUtil.isItSafe
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.Location
import org.bukkit.entity.Player

object EventSetPortalSubCommand {

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

        when(type) {
            EventPortalType.EVENT -> {
                sessionEvent.eventPortalLocation = location
                if(currentEvent == null) return

                val portalCuboid = currentEvent.eventPortalCuboid!!
                val portalLocation = currentEvent.eventPortalLocation!!

                currentEvent.removePortal(portalLocation, portalCuboid)
                currentEvent.removeEventPortalHologram()

                currentEvent.eventPortalLocation = location
                currentEvent.placeEventPortal()

                currentEvent.eventPortalLocation = location
            }
            EventPortalType.NORMAL -> {
                sessionEvent.portalLocation = location
                if(currentEvent == null) return

                val portalCuboid = currentEvent.portalCuboid!!
                val portalLocation = currentEvent.portalLocation!!

                currentEvent.removePortal(portalLocation, portalCuboid)
                currentEvent.removePortalHologram()

                currentEvent.portalLocation = location
                currentEvent.placePortal()

                currentEvent.portalLocation = location
            }
        }
    }
    private fun getLocationFrom(player: Player, args: Array<String>): Location = if(args.size == 5)
        Location(player.location.world, args[2].toDouble(), args[3].toDouble(), args[4].toDouble()) else player.location
}
enum class EventPortalType {
    EVENT, NORMAL
}