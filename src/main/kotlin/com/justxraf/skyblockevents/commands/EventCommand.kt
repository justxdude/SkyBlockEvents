package com.justxraf.skyblockevents.commands

import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.commands.Command
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventType

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class EventCommand : Command("event", arrayOf("e"), "hyperiol.events.admin") {

    private val eventsManager = EventsManager.instance
    private val commands = listOf(
        "create",
        "session",
        "start",
        "getregenerative",

        "description",
        "entity",
        "quests",
        "setportal",
        "setspawn"
    )
    private val sessionCommands = listOf(
        "description",
        "entity",
        "quests",
        "setportal",
        "setspawn"
    )

    override fun canExecute(player: Player, args: Array<String>): Boolean {
        val usageNoSession = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "",
            "&7> &c/event create <typ> &7- Tworzy nowe wydarzenie o podanym typie.",
            "&7> &c/event session &7- Otwiera informacje o sesjach.",
            "&7> &c/event start <id> &7- Zaczyna wydarzenie o podanym identyfikatorze.",
            "&7> &c/event getregenerative &7- Otwiera informacje o regenerujących blokach i roślinach.",
            "&c&m-".repeat(30),
        )
        val usageSession = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "",
            "&7> &c/event create <typ> &7- Tworzy nowe wydarzenie o podanym typie.",
            "&7> &c/event session &7- Otwiera informacje o sesjach.",
            "&7> &c/event start <id> &7- Zaczyna wydarzenie o podanym identyfikatorze.",
            "&7> &c/event getregenerative &7- Otwiera informacje o regenerujących blokach i roślinach.",
            "",
            "&7> &c/event description &7- Otwiera informacje o opisach",
            "&7> &c/event entity &7- Otwiera informacje o jednostkach",
            "&7> &c/event quests &7- Otwiera informacje o zadaniach",
            "&7> &c/event setportal &7- Otwiera informacje o ustawianiu portali",
            "&7> &c/event setspawn &7- Ustawia spawn dla wydarzenia",
            "&c&m-".repeat(30),
        )
        val isInSession = eventsManager.editSession.containsKey(player.uniqueId)
        if(args.isEmpty()) {
            if(!isInSession)
                usageNoSession.forEach { player.sendColoured(it) }
            else
                usageSession.forEach { player.sendColoured(it) }
            return false
        }
        if(!commands.contains(args[0])) {
            if(!isInSession)
                usageNoSession.forEach { player.sendColoured(it) }
            else
                usageSession.forEach { player.sendColoured(it) }
            return false
        }
        if(sessionCommands.contains(args[0]) && !isInSession) {
            usageNoSession.forEach { player.sendColoured(it) }
            return false
        }
        return true
    }

    override fun execute(player: Player, args: Array<String>) {
        if(!eventsManager.editSession.containsKey(player.uniqueId)) {
            when(args[0]) {
                "create" -> EventCreateSubCommand.process(player, args)
                "session" -> EventEditSubCommand.process(player, args)
                "start" -> EventStartSubCommand.process(player, args)
                "getregenerative" -> EventRegenerativeToolSubCommand.process(player, args)
            }

        } else {
            val eventData = eventsManager.editSession[player.uniqueId] ?: return
            val currentEvent = if(eventData.uniqueId == eventsManager.currentEvent.uniqueId) eventsManager.currentEvent else null

            when(args[0]) {
                "start" -> EventStartSubCommand.process(player, args)
                "getregenerative" -> EventRegenerativeToolSubCommand.process(player, args)
                "create" -> EventCreateSubCommand.process(player, args)
                "session" -> EventEditSubCommand.process(player, args)
                "description" -> EventDescriptionSubCommand.process(player, args, eventData, currentEvent)
                "entity" -> EventEntitySpawnPointSubCommand.process(player, args, eventData, currentEvent)
                "quests" -> EventQuestSubCommand.process(player, args, eventData, currentEvent)
                "setportal" -> EventSetPortalSubCommand.processSetPortal(player, args, eventData, currentEvent)
                "setspawn" -> EventSetSpawnSubCommand.process(player, args, eventData)
            }
        }
    }
    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val uniqueId = sender.asUser()?.uniqueId ?: return emptyList()
        val isInSession = eventsManager.editSession.containsKey(uniqueId)
        if(args.size == 1) {
            val nonSessionCommands = listOf("create", "session", "start", "getregenerative")
            val sessionCommands = listOf("create", "session", "start", "getregenerative", "description", "entity", "quests", "setportal", "setspawn")

            return if(isInSession) sessionCommands else nonSessionCommands
        }
        if(args.size == 2) {
            return when(args[0]) {
                "create" -> EventType.entries.map { it.name.lowercase() }
                "session" -> listOf("create", "destroy")
                "description" -> listOf("clear", "add", "removelastline")
                "entity" -> listOf("spawnpoint")
                "quests" -> listOf("add", "remove", "setnpc", "clear", "list")
                "getregenerative" -> listOf("plant", "block")
                "setportal" -> EventPortalType.entries.map { it.name.lowercase() }
                "start" -> eventsManager.events.map { it.value.uniqueId.toString() }
                "setspawn" -> listOf("region")
                else -> emptyList()
            }
        }
        if(args.size == 3) {
            return when(args[1]) {
                "create" -> eventsManager.events.map { it.value.uniqueId.toString() }
                "spawnpoint" -> listOf("remove", "create")
                else -> emptyList()
            }
        }
        if(args.size == 4) {
            return when(args[2]) {
                "create" -> EntityType.entries.map { it.name.lowercase() }

                else -> emptyList()
            }
        }
        return emptyList()
    }
}