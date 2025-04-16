package com.justxraf.skyblockevents.rewards

import com.justxdude.skyblockapi.rewards.Reward
import com.justxdude.skyblockapi.user.User
import com.justxdude.skyblockapi.user.UserExtensions.asPlayer
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.eventsTranslation

class PointsReward(
    val amount: Int
) : Reward() {
    override fun sendRewardToUser(user: User) {
        val eventsManager = EventsManager.instance
        val currentEvent = eventsManager.currentEvent

        val pointsHandler = currentEvent.pointsHandler
        pointsHandler.addPoints(user.uniqueId, amount)

        user.asPlayer()?.let { player ->
            player.sendColoured("received.points".eventsTranslation(player, amount.toString()))
        }
    }

    override fun copy(): Reward = PointsReward(amount)
}