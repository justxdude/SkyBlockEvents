package com.justxraf.skyblockevents.commands

import com.justxdude.networkapi.commands.Command
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getFormattedName
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class EventCommand : Command("event", arrayOf("e"), "hyperiol.skyblockevents.event") {

    private val eventsManager = EventsManager.instance

    private val eventSession = mutableMapOf<UUID, Int>()
    private val sessionCommands = listOf("SETSPAWN", "SETPORTAL", "SETQUESTNPC", "ADDQUEST", "ADDTODESCRIPTION", "CLEARDESCRIPTION")
    /*

    TODO:
     /event setentityspawnpoint <EntityType> (requires pos1 and pos2 with worldedit),
     /event seteventportal - Sets the location of the portal in the event world to come back to spawn,
     /event getregenerativeblock <material> - Gives a block to the player which can be placed and is added to the list of regenerative blocks
     /event getregenerativeblockremover - Gives a tool which can remove regenerative blocks
     /event removeentityspawnpoint - Removes the spawnpoint for entity in the session event

    Structure:

    /event
    /event create <type>
    /event edit <id> // opens the editing session for a given event
    /event destroysession

    /event setspawn
    /event setportal
    /event setquestnpc // for location only
    /event addquest
    /event addtodescription <string...> // adds to the existing description
    /event cleardescription // clears the description

     */

    override fun canExecute(player: Player, args: Array<String>): Boolean {
        if(args.isEmpty()) {
            player.sendColoured("&cNiepoprawne użycie! Dostępne komendy:")
            player.sendColoured("&c/event create <typ>, " +
                    "/event start <id>, " +
                    "/event edit <id>, " +
                    "/event setspawn, " +
                    "/event setportal, " +
                    "/event setquestnpc, " +
                    "/event addquest <id>, " +
                    "/event addtodescription <tekst>, " +
                    "/event cleardescription")
            return false
        }
        if(sessionCommands.contains(args[0].uppercase()) && !eventSession.contains(player.uniqueId)) {
            player.sendColoured("&cMusisz najpierw otworzyć sesję edytowania wydarzenia, " +
                    "aby użyć /event ${args[0]}! Użyj /event edit <id> lub /event create <typ>.")
            return false
        } else if(sessionCommands.contains(args[0]) && eventSession.contains(player.uniqueId)) {
            val oneArgCommands = listOf("SETSPAWN", "SETPORTAL", "ADDQUEST", "CLEARDESCRIPTION")
            if(oneArgCommands.contains(args[0].uppercase())) return true else {
                if(args.size < 2) {
                    player.sendColoured("&cMusisz użyć przynajmniej dwóch argumentów, " +
                            "aby dodać opis do tego wydarzenia! Użyj /event addtodescription <opis>.")
                    return false
                }
                if(args[0].uppercase() == "addquest") {
                    try {
                        val id = args[1].toInt()
                    }catch (e: NumberFormatException) {
                        player.sendColoured("&cUżyj numerów jako ID zadania!")
                        return false
                    }
                    return true
                }
                return true
            }
        }
        if(args[0].uppercase() == "START") {
            if(args.size < 2) {
                player.sendColoured("&cZła ilość argumentów! Użyj /event start <id>.")
                return false
            }
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
        if(args[0].uppercase() == "CREATE") {
            if(args.size < 2) {
                player.sendColoured("&cZła ilość argumentów! Użyj /event create <typ>.")
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
        if(args[0].uppercase() == "EDIT") {
            if(args.size < 2) {
                player.sendColoured("&cZła ilość argumentów! Użyj /event edit <id>.")
                return false
            }
            try {
                val id = args[1].toInt()
                if(eventsManager.currentEvent.uniqueId == id) {
                    player.sendColoured("&cNie możesz edytować tego wydarzenia, ponieważ jest ono obecnie aktywne!")
                    return false
                }

                val event = eventsManager.events[id]
                if(event == null) {
                    player.sendColoured("&cWydarzenie o tym ID nie istnieje! Użyj /event edit <id>.")
                    return false
                }

                return true
            } catch (e: NumberFormatException) {
                player.sendColoured("&cUżyj tylko liczb jako drugi argument! Użyj /event edit <id>.")
                return false
            }
        }
        if(args[0].uppercase() == "DESTROYSESSION") {
            if(!eventSession.contains(player.uniqueId)) {
                player.sendColoured("&cNie jesteś obecnie w żadnej sesji edytowania! Użyj /event edit <id> lub /event create <typ>.")
                return false
            }
            return true
        }
        return true
    }

    override fun execute(player: Player, args: Array<String>) {

        if(args[0].uppercase() == "START") processStart(player, args)
        if(args[0].uppercase() == "CREATE") processCreate(player, args)
        if(args[0].uppercase() == "EDIT") processEdit(player, args)

        if(args[0].uppercase() == "DESTROYSESSION") processDestroySession(player)
        if(sessionCommands.contains(args[0].uppercase())) processSessionCommands(player, args)

    }
    private fun processStart(player: Player, args: Array<String>) {
        eventsManager.currentEvent.end()

        val event = eventsManager.events[args[1].toInt()]?.fromData() ?: return
        eventsManager.currentEvent = event
        event.start()
    }
    private fun processSessionCommands(player: Player, args: Array<String>) {
        val event = eventsManager.events[eventSession[player.uniqueId]] ?: return

        when(args[0].uppercase()) {
            "SETSPAWN" -> {
                event.spawnLocation = player.location
                player.sendColoured("&aZmieniono spawn dla wydarzenia ${event.name} na ${player.location.x} ${player.location.y} ${player.location.z} (XYZ)")
            }
            "SETPORTAL" -> {
                event.portalLocation = player.location
                player.sendColoured("&aZmieniono lokację portalu na ${event.name} na ${player.location.x} ${player.location.y + 1} ${player.location.z} (XYZ)")
            }
            "SETQUESTNPC" -> {
                event.questNPCLocation = player.location
                player.sendColoured("&aZmieniono lokalizację NPC z zadaniami dla wydarzenia ${event.name} na ${player.location.x} ${player.location.y} ${player.location.z} (XYZ)")
            }
            "ADDQUEST" -> {
                event.quests?.plusAssign(args[1].toInt())
                player.sendColoured("&aDodano zadanie o ID ${args[1]} dla wydarzenia ${event.name}")
            }
            "ADDTODESCRIPTION" -> {
                val message = args.slice(1 until args.size).joinToString(" ")
                event.description += message

                player.sendColoured("&aDodano nową linię do opisu dla wydarzenia ${event.name} o treści: $message")
            }
            "CLEARDESCRIPTION" -> {
                event.description.clear()
                player.sendColoured("&aWyczyszczono opis dla wydarzenia ${event.name}. Dodaj nowy opis przy uzyciu komendy /event addtodescription <treść>")
            }
            else -> return
        }
        eventsManager.saveEvent(event)
    }
    private fun processEdit(player: Player, args: Array<String>) {
        val id = args[1].toInt()
        eventSession[player.uniqueId] = id
        player.sendColoured("&aPomyślnie dodano Cię do sesji edytowania wydarzenia o id ${eventSession[player.uniqueId]}.")
    }
    private fun processDestroySession(player: Player) {
        player.sendColoured("&aPomyślnie usunięto z sesji edytowania wydarzenia o id ${eventSession[player.uniqueId]}.")
        eventSession.remove(player.uniqueId)
    }
    private fun processCreate(player: Player, args: Array<String>) {
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
            else -> {
                EventData(
                    type.getFormattedName(),
                    lastIP + 1,
                    type,
                    0,
                    0,
                    location.world!!.name,
                    mutableListOf(""),
                    location)
            }
        }
        eventsManager.events[event.uniqueId] = event
        eventsManager.saveEvent(event)

        eventSession[player.uniqueId] = event.uniqueId
        player.sendColoured("&aPomyślnie utworzono wydarzenie ${event.name} (id ${event.uniqueId}).")
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val user = sender.asUser() ?: return emptyList()
        val session = eventSession[user.uniqueId]
        if(session == null && args.size == 1) return listOf("create", "edit", "destroysession")
        if(args.size == 1) return listOf("create", "edit", "destroysession", "setspawn", "setportal",
            "setquestnpc", "addquest", "addtodescription", "cleardescription")
        val events = EventsManager.instance.events.map { it.key.toString() }
        if(args.size == 2) return if(args[0] == "create") EventType.entries.map { it.getFormattedName() }
        else if(args[0] == "edit") events else emptyList()

        return emptyList()
    }
}