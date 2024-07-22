package com.justxraf.skyblockevents.commands

import com.justxdude.islandcore.utils.toLocationString
import com.justxdude.networkapi.util.LocationUtil.isItSafe
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.Location
import org.bukkit.entity.Player

object EventSetPortalSubCommand {
    private fun shouldProcess(player: Player, sessionEvent: EventData, args: Array<String>): Boolean {
        val location = getLocationFrom(player, args)
        if(location.world == sessionEvent.spawnLocation.world) {
            player.sendColoured("&cNie możesz ustawić portalu do wydarzenia w tym samym świecie, w którym jest wydarzenie.")
            return false
        }
        if(!location.isItSafe()) {
            player.sendColoured("&cTa lokacja nie jest bezpieczna, aby postawić portal. Spróbuj innego miejsca.")
            return false
        }
        return true
    }
    fun processSetPortal(player: Player, sessionEvent: EventData, args: Array<String>) {
        if(!shouldProcess(player, sessionEvent, args)) return
        val location = getLocationFrom(player, args)

        sessionEvent.eventPortalLocation = player.location
        player.sendColoured("&aUstawiono portal wydarzenia w lokacji ${location.toLocationString()} (XYZ).")
    }
    private fun getLocationFrom(player: Player, args: Array<String>): Location = if(args.size > 1)
        Location(player.location.world, args[1].toDouble(), args[2].toDouble(), args[3].toDouble()) else player.location

}