package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.entity.Player
import java.awt.SystemColor.text

object EventDescriptionSubCommand {
    private val eventsManager = EventsManager.instance
    /*

    /event description clear
    /event description add <args..>
    /event description removelastline

     */

    private fun shouldProcess(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        val message = listOf(
            "&c&m-".repeat(30),
            "&cNiepoprawne użycie! Użyj:",
            "",
            "&7> &c/event description clear &7- Oczyszcza cały opis wydarzenia",
            "&7> &c/event description add <tekst> &7- Dodaje kolejną linię do opisu.",
            "&7> &c/event description removelastline &7- Usuwa ostatnią linię z opisu.",
            "&c&m-".repeat(30)
        )
        if(args.size < 2) {
            message.forEach { player.sendColoured(it) }
            return false
        }

        when(args[1].lowercase()) {
            "clear", "removelastline" -> {
                if(sessionEvent.description.isEmpty()) {
                    player.sendColoured("&cOpis wydarzenia o identyfikatorze #${sessionEvent.uniqueId} jest pusty.")
                    return false
                }
                return true
            }
            "add" -> {
                if(args.size < 3) {
                    player.sendColoured("&cMusisz użyć przynajmniej trzech argumentów! Użyj /event add <tekst...>.")
                    return false
                }
                return true
            }
            else -> {
                message.forEach { player.sendColoured(it) }
                return false
            }
        }
    }
    fun process(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcess(player, args, sessionEvent)) return
        when(args[1].lowercase()) {
            "clear" -> {
                sessionEvent.description.clear()
                currentEvent?.description?.clear()

                player.sendColoured("&7Poprawnie wyczyszczono opis dla wydarzenia o identyfikatorze #${sessionEvent.uniqueId}.")
            }
            "removelastline" -> {
                val index = sessionEvent.description.size - 1
                val text = sessionEvent.description[index]

                sessionEvent.description.removeAt(index)
                currentEvent?.description?.removeAt(index)

                player.sendColoured("&7Poprawnie usunięto ostatnią linię dla wydarzenia o identyfikatorze #${sessionEvent.uniqueId}, o treści \"$text\"")
            }
            "add" -> {
                val text = args.slice(2..args.size).joinToString { " " }
                sessionEvent.description.add(text)

                currentEvent?.description?.add(text)
                player.sendColoured("&7Poprawnie dodano linię do opisu dla wydarzenia o identyfikatorze #${sessionEvent.uniqueId} o treści: \"$text\"")
            }
        }
    }
}