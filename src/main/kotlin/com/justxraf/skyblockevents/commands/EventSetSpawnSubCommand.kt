package com.justxraf.skyblockevents.commands

import com.justxdude.islandcore.utils.toLocationString
import com.justxraf.networkapi.util.LocationUtil.isItSafe
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.entity.Player

object EventSetSpawnSubCommand {
    private val eventsManager = EventsManager.instance

    private fun shouldProcess(player: Player, sessionEvent: EventData): Boolean {
        val location = player.location
        if(location.world != sessionEvent.spawnLocation.world) {
            player.sendColoured("&cNie możesz zmienić spawnu dla tego wydarzenia na inny świat. Utwórz nowe wydarzenie, aby zmienić świat.")
            return false
        }
        if(!location.isItSafe()) {
            player.sendColoured("&cTa lokacja nie jest bezpieczna! Użyj /event setspawn.")
            return false
        }
        return true
    }
    fun process(player: Player, sessionEvent: EventData) {
        if(!shouldProcess(player, sessionEvent)) return

        val location = player.location
        sessionEvent.spawnLocation = location
        val currentEvent = eventsManager.currentEvent

        if(currentEvent.uniqueId == sessionEvent.uniqueId) currentEvent.spawnLocation = location

        player.sendColoured("&aPoprawnie zmieniłeś/aś lokację dla wydarzenia ${sessionEvent.uniqueId} to ${location.toLocationString()} (XYZ).")
    }
}