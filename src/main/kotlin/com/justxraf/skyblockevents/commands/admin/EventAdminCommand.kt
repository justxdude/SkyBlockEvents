package com.justxraf.skyblockevents.commands.admin

import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.commands.Command
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventType

import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.portals.EventPortalType
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class EventAdminCommand : Command("eventadmin", arrayOf("ea"), "hyperiol.events.admin") {

    private val eventsManager = EventsManager.instance
    private val commands = listOf(
        "create",
        "session",
        "start",

        "description",
        "entity",
        "quests",
        "setportal",
        "setspawn",
        "reload",
        "setregenerative"
        // /eventadmin setregenerative <type>
    )
    private val sessionCommands = listOf(
        "description",
        "entity",
        "quests",
        "setportal",
        "setspawn",
        "reload",
        "setregenerative",
    )

    override fun canExecute(player: Player, args: Array<String>): Boolean {
        val usageNoSession = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "",
            "&7> &c/event create <typ> &7- Tworzy nowe wydarzenie o podanym typie.",
            "&7> &c/event session &7- Otwiera informacje o sesjach.",
            "&7> &c/event start <id> &7- Zaczyna wydarzenie o podanym identyfikatorze.",
            "&c&m-".repeat(30),
        )
        val usageSession = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "",
            "&7> &c/event create <typ> &7- Tworzy nowe wydarzenie o podanym typie.",
            "&7> &c/event session &7- Otwiera informacje o sesjach.",
            "&7> &c/event start <id> &7- Zaczyna wydarzenie o podanym identyfikatorze.",
            "",
            "&7> &c/event description &7- Otwiera informacje o opisach",
            "&7> &c/event entity &7- Otwiera informacje o jednostkach",
            "&7> &c/event quests &7- Otwiera informacje o zadaniach",
            "&7> &c/event setportal &7- Otwiera informacje o ustawianiu portali",
            "&7> &c/event setspawn &7- Ustawia spawn dla wydarzenia",
            "&7> &c/eventadmin setregenerative <typ> &7- Ustawia zaznaczone bloki danego typu jako regenerujące",
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
                "create" -> EventAdminCreateSubCommand.process(player, args)
                "session" -> EventAdminEditSubCommand.process(player, args)
                "start" -> EventAdminStartSubCommand.process(player, args)
            }

        } else {
            val eventData = eventsManager.editSession[player.uniqueId] ?: return
            val currentEvent = if(eventData.uniqueId == eventsManager.currentEvent?.uniqueId) eventsManager.currentEvent else null

            when(args[0]) {
                "start" -> EventAdminStartSubCommand.process(player, args)
                "setregenerative" -> EventAdminSetRegenerativeBlocksCommand.process(player, args, eventData)
                "create" -> EventAdminCreateSubCommand.process(player, args)
                "session" -> EventAdminEditSubCommand.process(player, args)
                "description" -> EventAdminDescriptionSubCommand.process(player, args, eventData, currentEvent)
                "entity" -> EventAdminEntitySpawnPointSubCommand.process(player, args, eventData, currentEvent)
                "quests" -> EventAdminQuestSubCommand.process(player, args, eventData, currentEvent)
                "setportal" -> EventAdminSetPortalSubCommand.processSetPortal(player, args, eventData, currentEvent)
                "setspawn" -> EventAdminSetSpawnSubCommand.process(player, args, eventData)
                "reload" -> {
                    EventsManager.instance.currentEvent.end()

                    EventsManager.instance.generateNewEvent()

                    player.sendColoured("&aPoprawnie zrestartowano obecne wydarzenie!")
                }
            }

            EventsManager.instance.saveEvent(eventData)
            EventsManager.instance.saveCurrentEvent()
        }
    }
    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val uniqueId = sender.asUser()?.uniqueId ?: return emptyList()
        val isInSession = eventsManager.editSession.containsKey(uniqueId)
        if(args.size == 1) {
            val nonSessionCommands = listOf("create", "session", "start", "reload")
            val sessionCommands = listOf("setregenerative","create", "session", "start", "description", "entity", "quests", "setportal", "setspawn", "reload")

            return if(isInSession) sessionCommands else nonSessionCommands
        }
        if(args.size == 2) {
            return when(args[0]) {
                "create" -> EventType.entries.map { it.name.lowercase() }
                "session" -> listOf("create", "destroy")
                "description" -> listOf("clear", "add", "removelastline")
                "entity" -> listOf("spawnpoint")
                "quests" -> listOf("add", "remove", "setnpc", "clear", "list", "reloadnpc")
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
                "create" -> EntityType.entries.map { it.name }

                else -> emptyList()
            }
        }
        return emptyList()
    }
}