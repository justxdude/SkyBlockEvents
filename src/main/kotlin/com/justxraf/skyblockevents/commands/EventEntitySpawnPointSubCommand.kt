package com.justxraf.skyblockevents.commands

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.entity.Player

class EventEntitySpawnPointSubCommand {

    // /event spawnpoint set
    // /event spawnpoint remove

    private fun shouldProcess(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        if(args.size < 2) {
            player.sendColoured("&cNiepoprawne użycie! Użyj /event spawnpoint <set/remove>")
            return false
        }
        val location = player.location
        if(location.world != sessionEvent.spawnLocation.world) {
            player.sendColoured("&cMusisz być w świecie wydarzenia ")
        }

        return true
    }
}