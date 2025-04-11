package com.justxraf.skyblockevents.commands

import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.guis.EventLeaderboardGUI
import org.bukkit.entity.Player

object EventLeaderboardCommand {
    // /event leaderboard todo - Add it later for event command, cannot access the newest version now.

    private val eventsManager = EventsManager.instance
    fun execute(player: Player) {
        val currentEvent = eventsManager.currentEvent
        val pointsHandler = currentEvent.pointsHandler

        if(pointsHandler.playersLeaderboard.isEmpty()) {
            player.sendColoured("&cTabela wyników jest pusta, ponieważ nikt jeszcze nie dołączył do tego wydarzenia.")
            return
        }

        val user = player.asUser() ?: return
        EventLeaderboardGUI(player, user).open(player)
    }
}