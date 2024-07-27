package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.entity.Player

object EventEditSubCommand {

    /*
    /event session create <id>
    /event session create
    /event session destroy
     */

    private val eventsManager = EventsManager.instance

    private fun shouldProcess(player: Player, args: Array<String>): EventData? {

        if(args.size == 3) {
            try {
                val event = eventsManager.events.firstNotNullOfOrNull { if(it.value.uniqueId == args[2].toInt()) it else null }?.value
                if(event == null) {
                    player.sendColoured("&cWydarzenie które wybrałeś(aś) nie istnieje! Użyj /event session create <id>.")
                    return null
                }
                return event
            } catch (e: NumberFormatException){
                player.sendColoured("&cIdentyfikator musi być numerem! Użyj /event session create <id>.")
                return null
            }
        }
        if(args.size == 2) {
            when (args[1].lowercase()) {
                "create" -> {
                    val event =
                        eventsManager.events.firstNotNullOfOrNull { if (it.value.spawnLocation.world == player.world) it else null }?.value
                    if (event == null) {
                        player.sendColoured("&cNie ma wydarzenia w świecie w którym obecnie przebywasz! Użyj /event session create.")
                        return null
                    }
                    return event
                }

                "destroy" -> {
                    if (eventsManager.editSession[player.uniqueId] == null) {
                        player.sendColoured("&cNie jesteś w żadnej sesji! Użyj /event session destroy.")
                    }
                    return eventsManager.editSession[player.uniqueId]
                }
            }
        }
        val usage = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "&c",
            "&7> &c/event session create <id> &7- Otwiera sesję edytowania dla wybranego wydarzenia.",
            "&7> &c/event session create &7- Otwiera sesję edytowania dla wydarzenie które jest w świecie w którym obecnie przebywasz.",
            "&7 &c/event session destroy &7- Zamyka sesję edytowania dla wydarzenia jeżeli jesteś w sesji.",
            "&c&m-".repeat(30)
        )
        usage.forEach { player.sendColoured(it) }
        return null
    }

    fun process(player: Player, args: Array<String>) {
        val eventData = shouldProcess(player, args) ?: return
        when(args[1]) {
            "create" -> {
                eventsManager.editSession[player.uniqueId] = eventData
                player.sendColoured("&7Otworzono sesję edytowania dla wydarzenia o identyfikatorze #${eventData.uniqueId}.")
            }
            else -> {
                eventsManager.editSession.remove(player.uniqueId)
                player.sendColoured("&7Zamknięto sesję edytowania dla wydarzenia o identyfikatorze #${eventData.uniqueId}.")
            }
        }
    }
}