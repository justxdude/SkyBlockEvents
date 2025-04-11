package com.justxraf.skyblockevents.commands

import com.justxdude.islandcore.utils.toLocationString
import com.justxraf.networkapi.util.LocationUtil.isItSafe
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.SelectionAnswer
import com.justxraf.skyblockevents.util.getWorldEditSelection
import com.justxraf.skyblockevents.util.hasWorldEditSelection
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
    private fun shouldProcessSetRegion(player: Player): Boolean {
        val selection = player.hasWorldEditSelection()
        val selectionAnswer = selection.firstNotNullOfOrNull { it.key } ?: return false

        return selectionAnswer == SelectionAnswer.CORRECT
    }

    fun process(player: Player,args: Array<String>, sessionEvent: EventData) {
        if(!shouldProcess(player, sessionEvent)) return
        val currentEvent = eventsManager.currentEvent

        if(args.size > 1 && args[1].lowercase() == "region") {
            if (!shouldProcessSetRegion(player)) return
            val (loc1, loc2) = player.getWorldEditSelection() ?: return

            sessionEvent.spawnRegion = Pair(loc1, loc2)
            if (currentEvent.uniqueId == sessionEvent.uniqueId) currentEvent.spawnRegion = Pair(loc1, loc2)
        }

        val location = player.location
        sessionEvent.spawnLocation = location

        if(currentEvent.uniqueId == sessionEvent.uniqueId) currentEvent.spawnLocation = location

        player.sendColoured("&aPoprawnie zmieniłeś/aś lokację dla wydarzenia ${sessionEvent.uniqueId} to ${location.toLocationString()} (XYZ).")
    }
}