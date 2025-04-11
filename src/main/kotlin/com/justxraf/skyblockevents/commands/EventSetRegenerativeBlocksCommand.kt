package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.Material
import org.bukkit.entity.Player

object EventSetRegenerativeBlocksCommand {
    // /eventadmin setregenerative <type>
    private fun shouldProcess(player: Player, args: Array<String>): Material? {
        if(args.size < 2) {
            player.sendColoured("&cNiepoprawne użycie! Użyj /event setregenerative <typ> <true/false>.")
            return null
        }
        val type = Material.entries.firstOrNull { it.name == args[1].uppercase() }
        if(type == null) {
            player.sendColoured("&cNiepoprawny blok! Użyj /eventadmin setregenerative <typ> <true/false>.")
        }
        if(args.size != 3) {
            player.sendColoured("&cNiepoprawne użycie! Użyj /event setregenerative <typ> <true/false>.")
            return null
        }
        if(args[2] != "true" && args[2] != "false") {
            player.sendColoured("&cWpisz true lub false na końcu komendy! Pozwala to na ustalenie tego czy dany blok jest rośliną.")
            return null
        }
        return type
    }
    fun process(player: Player,args: Array<String>, sessionEvent: EventData) {
        val type = shouldProcess(player, args) ?: return

        sessionEvent.addRegenerativeMaterial(type, args[2].contains("true"))

        val currentEvent = EventsManager.instance.currentEvent
        if(currentEvent.uniqueId == sessionEvent.uniqueId) {
            currentEvent.regenerativeMaterialsManager.addRegenerativeMaterial(type, args[2].contains("true"))
        }

        player.sendColoured("&7Dodano materiał ${type.name} do wydarzenia ${sessionEvent.uniqueId}!")
        player.sendColoured("&7Obecna ilość materiałów to: ${sessionEvent.regenerativeMaterials?.size}")
    }
}
