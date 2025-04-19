package com.justxraf.skyblockevents.commands.players

import com.justxdude.skyblockapi.rewards.RewardsHandler
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.getRankColor
import org.bukkit.entity.Player

object EventRewardsSubCommand {
    private val eventsManager = EventsManager.Companion.instance
    fun execute(player: Player) {
        val currentEvent = eventsManager.currentEvent
        val playerRewards = currentEvent.eventUserHandler.playerRewards
        val rewardsHandler = RewardsHandler.instance

        player.sendColoured("\n&cNagrody dla graczy:\n")
        playerRewards.forEach { (pos, rewards) ->
            val color = getRankColor(pos)

            val message = mutableListOf("${color}Nagrody za $pos miejsce:")
            message.addAll(rewards.mapNotNull { rewardsHandler.getReward(it) }.flatMap { it.getDescription(player) })
            val wholeMessage = message.joinToString("\n")

            player.sendColoured(wholeMessage + "\n")
        }
        val islandRewards = currentEvent.eventUserHandler.islandRewards
        val islandMessage = mutableListOf("&cNagrody dla Najlepszej Wyspy:")
        islandMessage.addAll(islandRewards.mapNotNull { rewardsHandler.getReward(it) }.flatMap { it.getDescription(player) })

        player.sendColoured(islandMessage.joinToString("\n"))
    }
}