package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getFormattedName
import org.bukkit.entity.Player

object EventCreateSubCommand {
    private val eventsManager = EventsManager.instance
    // /event create <type>

    private fun shouldProcess(player: Player, args: Array<String>): Boolean {
        if (args.size < 2) {
            player.sendColoured("&cZła ilość argumentów! Użyj /event create <typ>.")
            return false
        }
        if(eventsManager.events.any { it.value.spawnLocation.world == player.world }) {
            player.sendColoured("&cW tym świecie już istnieje wydarzenie!")
            return false
        }
        try {
            EventType.valueOf(args[1].uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendColoured("&cNiepoprawny typ wydarzenia! Użyj /event create <typ>.")
            return false
        }
        return true
    }
    fun process(player: Player, args: Array<String>) {
        if(!shouldProcess(player, args)) return
        val type = EventType.valueOf(args[1].uppercase())

        val location = player.location
        val lastIP = eventsManager.events.keys.maxOrNull() ?: 1

        val event = when(type) {
            EventType.NETHER -> EventData(
                type.getFormattedName(),
                lastIP + 1,
                type,
                0,
                0,
                location.world!!.name,
                mutableListOf(""),
                location)
            else -> EventData(type.getFormattedName(), lastIP + 1, type, 0, 0, location.world!!.name, mutableListOf(""), location)

        }
        eventsManager.events[event.uniqueId] = event
        eventsManager.saveEvent(event)

        eventsManager.editSession[player.uniqueId] = event
        player.sendColoured("&aPomyślnie utworzono wydarzenie ${event.name} #${event.uniqueId}.")
    }
}