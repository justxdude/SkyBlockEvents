package com.justxraf.skyblockevents.commands.admin

import com.justxraf.networkapi.util.LocationUtil.isItSafe
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.questscore.quests.QuestsManager
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.entity.Player

object EventAdminQuestSubCommand {
    private val questsManager = QuestsManager.instance
    private val eventsManager = EventsManager.instance
    /*
    /event quests add <id>
    /event quests remove <id>
    /event quests setnpc
    /event quests clear // resets all quests
    /event quests list
     */
    private fun shouldProcess(player: Player, args: Array<String>): Boolean {
        when(args[1].lowercase()) {
            "setnpc", "list", "clear", "reloadnpc" -> if(args.size == 2) return true
            "add", "remove" -> if(args.size == 3) return true
        }
        val usage = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "",
            "&e> &c/event quest add <id> &7- Dodaje zadanie do wydarzenia.",
            "&e> &c/event quest remove <id> &7- Usuwa zadanie z wydarzenia.",
            "&e> &c/event quest setnpc &7- Ustawia respawn dla NPC w Twojej lokacji.",
            "&c&m-".repeat(30)
        )
        usage.forEach {
            player.sendColoured(it)
        }
        return false
    }
    private fun shouldProcessList(player:Player, args: Array<String>, sessionEvent: EventData): Boolean {
        try {

            if(sessionEvent.quests.isNullOrEmpty()) {
                player.sendColoured("&cLista zadań dla wydarzenia #${sessionEvent.uniqueId} jest pusta!")
                player.sendColoured("&cDodaj nowe zadanie przy użyciu komendy /event quests add <id>.")
                return false
            }
            return true

        }catch (e: Exception) {
            e.printStackTrace()
            player.sendColoured("&cWystąpił błąd podczas wykonywania operacji.")
            return false
        }
    }
    private fun shouldProcessClear(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        val usage = "Użyj /event quests clear."
        try {
            if(sessionEvent.quests.isNullOrEmpty()) {
                player.sendColoured("&cLista zadań dla wydarzenia #${sessionEvent.uniqueId} jest już pusta!")
                return false
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            player.sendColoured("&cWystąpił błąd podczas wykonywania operacji! $usage")
            return false
        }
    }
    private fun shouldProcessAdd(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        try {
            if(sessionEvent.quests.isNullOrEmpty()) sessionEvent.quests = mutableListOf()

            val id = args[2].toInt()
            val quest = questsManager.getQuestBy(id)

            if(quest == null) {
                player.sendColoured("&cNie znaleziono zadania o podanym id! Użyj /event quests add <id>.")
                return false
            }
            if(sessionEvent.quests?.contains(id) == true) {
                player.sendColoured("&cTe zadanie jest już dodanie! Użyj /event quests add <id>.")
                return false
            }

        } catch (exception: NumberFormatException) {
            player.sendColoured("&cUżyj numeru w trzecim argumencie! Użyj /event quests add <id>.")
            return false
        }

        return true
    }
    private fun shouldProcessRemove(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        val usage = "Użyj /event quests remove <id>."

        try {
            if(sessionEvent.quests.isNullOrEmpty()) {
                player.sendColoured("&cTe wydarzenie nie ma żadnych dodanych zadań. $usage")
                return false
            }
            val id = args[2].toInt()
            if(sessionEvent.quests?.contains(id) == false) {
                player.sendColoured("&cNie ma tego zadania w tym wydarzeniu! $usage")
            }
            return true

        }catch (e: NumberFormatException) {
            player.sendColoured("&cUżyj numeru w trzecim argumencie! $usage")
            return false
        }
    }
    private fun shouldProcessSetNpc(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        val usage = "Użyj /event quests setnpc."
        try {
            val location = player.location
            if(location.world != sessionEvent.spawnLocation.world) {
                player.sendColoured("&cMusisz być w tym samym świecie w którym jest wydarzenie! $usage")
                return false
            }
            if(!location.isItSafe()) {
                player.sendColoured("&cTa lokacja nie jest bezpieczna! Przejdź w inne miejsce i spróbuj ponownie. $usage")
                return false
            }
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            player.sendColoured("&cWystąpił błąd podczas wykonywania operacji.")
            return false
        }
    }
    private fun shouldProcessReloadNPC(player: Player, sessionEvent: EventData): Boolean {
        val currentEvent = eventsManager.currentEvent
        if(currentEvent?.uniqueId != sessionEvent.uniqueId) {
            player.sendColoured("&cMożesz zresetować NPC tylko gdy te wydarzenie jest aktywne.")
            return false
        }
        if(sessionEvent.questNPCLocation == null) {
            player.sendColoured("&cWydarzenie #${sessionEvent.uniqueId} nie ma ustawionego NPC dla zadań. " +
                    "Ustaw je przy użyciu komendy /event quests setnpc.")
            return false
        }
        return true
    }
    private fun processReloadNPC(player: Player, sessionEvent: EventData) {
        if(!shouldProcessReloadNPC(player, sessionEvent)) return

        val currentEvent = eventsManager.currentEvent

        currentEvent?.removeNPC()
        currentEvent?.spawnQuestNPC()

        player.sendColoured("&7Zrestartowano NPC w wydarzeniu #${sessionEvent.uniqueId}.")
    }
    private fun processList(player: Player, args: Array<String>, sessionEvent: EventData) {
        if(!shouldProcessList(player, args, sessionEvent)) return
        val message = sessionEvent.quests?.joinToString(", ")

        player.sendColoured("&7Zadania w wydarzeniu #${sessionEvent.uniqueId}: $message.")
    }
    private fun processClear(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessClear(player, args, sessionEvent)) return

        sessionEvent.quests?.clear()
        val quests = currentEvent?.quests

        quests?.forEach {
            currentEvent.removeQuest(it)
        }

        player.sendColoured("&7Poprawnie wyczyszczono listę zadań dla wydarzenia #${sessionEvent.uniqueId}" +
                if(!quests.isNullOrEmpty()) " (usunięto ${quests.size} zadań)." else "")
    }
    private fun processAdd(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessAdd(player, args, sessionEvent)) return

        sessionEvent.addQuest(args[2].toInt())
        currentEvent?.addQuest(args[2].toInt())

        player.sendColoured("&7Poprawnie dodałeś(aś) zadanie ${args[2]} do wydarzenia #${sessionEvent.uniqueId}" +
                if(currentEvent != null) " (aktywne wydarzenie zostało rónież zmodyfikowane, ponieważ ma ten sam identyfikator)." else ""
        )
    }
    private fun processRemove(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessRemove(player, args, sessionEvent)) return

        sessionEvent.quests?.remove(args[2].toInt())
        currentEvent?.removeQuest(args[2].toInt())

        player.sendColoured("&7Poprawnie usunąłeś(aś) zadanie ${args[2]} z wydarzenia #${sessionEvent.uniqueId}" +
                if(currentEvent != null) " (aktywne wydarzenie zostało rónież zmodyfikowane, ponieważ ma ten sam identyfikator)." else ""
        )
    }
    private fun processSetNpc(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessSetNpc(player, args, sessionEvent)) return

        sessionEvent.questNPCLocation = player.location
        currentEvent?.questNPCLocation = player.location

        currentEvent?.removeNPC()
        currentEvent?.spawnQuestNPC()

        player.sendColoured("&7Poprawnie zmieniłeś lokację NPC z zadaniami dla wydarzenia #${sessionEvent.uniqueId}."
            + if(currentEvent != null) " (aktywne wydarzenie zostało rónież zmodyfikowane, ponieważ ma ten sam identyfikator)" else "")
    }
    fun process(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcess(player, args)) return
        when(args[1].lowercase()) {
            "add" -> processAdd(player, args, sessionEvent, currentEvent)
            "remove" -> processRemove(player, args, sessionEvent, currentEvent)
            "setnpc" -> processSetNpc(player, args, sessionEvent, currentEvent)
            "clear" -> processClear(player, args, sessionEvent, currentEvent)
            "list" -> processList(player, args, sessionEvent)
            "reloadnpc" -> processReloadNPC(player, sessionEvent)
        }
    }
}