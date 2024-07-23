package com.justxraf.skyblockevents.commands

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.entity.Player

object EventDescriptionSubCommand {
    // /event description add <args..>
    // /event description clear
    private fun shouldProcess(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        if(args.size < 2) {
            player.sendColoured("&cNiepoprawne użycie! Użyj /event description <add/clear> <nowy-opis/nic>")
            return false
        }
        if(args[1].lowercase() == "add") {
            if(args.size < 3) {
                player.sendColoured("&cMusisz użyć przynajmniej jednego słowa!")
                return false
            }
            return true
        }
        if(args[1].lowercase() == "clear") {
            if(sessionEvent.description.isEmpty()) {
                player.sendColoured("&cOpis dla tego wydarzenia jest już pusty.")
                return false
            }
            return true
        }
        return true
    }
    fun process(player: Player, args: Array<String>, sessionEvent: EventData) {
        if(!shouldProcess(player, args, sessionEvent)) return

        if(args[1].lowercase() == "add") processAddDescription(player, args, sessionEvent)
        if(args[1].lowercase() == "clear") processClearDescription(player, args, sessionEvent)
    }
    private fun processAddDescription(player: Player, args: Array<String>, sessionEvent: EventData) {
        val description = args.slice(2 until args.size).joinToString(" ")
        sessionEvent.description += description

        player.sendColoured("&aDodałeś/aś nową linię do wydarzenia o identyfikatorze ${sessionEvent.description} o treści \"$description\".")
    }
    private fun processClearDescription(player: Player, args: Array<String>, sessionEvent: EventData) {
        sessionEvent.description.clear()
        player.sendColoured("&aOczyściłeś/aś opis dla wydarzenia o identyfikatorze ${sessionEvent.uniqueId}.")
    }
}