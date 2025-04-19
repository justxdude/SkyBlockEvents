package com.justxraf.skyblockevents.rewards

import com.justxdude.skyblockapi.rewards.reward.Reward
import com.justxdude.skyblockapi.user.User
import com.justxdude.skyblockapi.user.UserExtensions.asPlayer
import com.justxdude.skyblockapi.utils.Util.translate
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.eventsTranslation
import org.bukkit.entity.Player

class PointsReward(
    override val uniqueId: Int,
    val amount: Int
) : Reward(uniqueId) {
    override fun sendRewardToUser(user: User, shouldSendMessage: Boolean) {
        val eventsManager = EventsManager.instance
        val currentEvent = eventsManager.currentEvent

        val eventUserHandler = currentEvent.eventUserHandler
        val eventUser = eventUserHandler.getUser(user.uniqueId)

        eventUser.addPoints(amount)

        user.asPlayer()?.let { player ->
            player.sendColoured("received.points".eventsTranslation(player, amount.toString()))
        }
    }

    override fun copy(): Reward = PointsReward(uniqueId, amount)
    override fun getDescription(player: Player): List<String> = listOf("&8- &7${amount}x ${"event.points".translate(player)}")
}