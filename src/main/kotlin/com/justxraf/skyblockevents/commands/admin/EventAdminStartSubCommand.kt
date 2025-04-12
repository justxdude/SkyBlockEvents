package com.justxraf.skyblockevents.commands.admin

import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.entity.Player

object EventAdminStartSubCommand {
    // /event start <id>.

    private val eventsManager = EventsManager.instance

    private fun shouldProcess(player: Player, args: Array<String>): Boolean {
        try {
            val event = eventsManager.events[args[1].toInt()]
            if(event == null) {
                player.sendColoured("&cTe wydarzenie nie istnieje! Użyj /event start <id>.")
                return false
            }
            if(eventsManager.currentEvent.uniqueId == args[1].toInt()) {
                player.sendColoured("&cTe wydarzenie już wystartowało! Użyj /event start <id>.")
                return false
            }
            return true

        } catch (e: NumberFormatException) {
            player.sendColoured("&cMożesz użyć tylko liczb w drugim argumencie! Użyj /event start <id>.")
            return false
        }
    }
    fun process(player: Player, args: Array<String>) {
        if(!shouldProcess(player, args)) return
        eventsManager.currentEvent.end()

        val event = eventsManager.events[args[1].toInt()]?.fromData() ?: return
        eventsManager.currentEvent = event
        event.start()
    }
}