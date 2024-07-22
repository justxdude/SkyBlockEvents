package com.justxraf.skyblockevents.commands

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.data.EventData
import org.bukkit.Location
import org.bukkit.entity.Player

object EventSetQuestNpcSubCommand {
    private fun shouldProcess(player: Player, sessionEvent: EventData): Boolean {
        val location = player.location
        if(location.world != sessionEvent.spawnLocation.world) {
            player.sendColoured("&cMusisz znajdować się w tym samym świecie, w którym jest wydarzenie.")
            return false
        }

        return true
    }
    fun process(player: Player, sessionEvent: EventData) {
        if(!shouldProcess(player, sessionEvent)) return
    }
}