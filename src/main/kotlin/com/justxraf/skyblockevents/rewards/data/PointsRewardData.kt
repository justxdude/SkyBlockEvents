package com.justxraf.skyblockevents.rewards.data

import com.justxdude.skyblockapi.rewards.data.RewardData
import com.justxdude.skyblockapi.rewards.reward.Reward
import com.justxraf.skyblockevents.rewards.PointsReward

data class PointsRewardData(val amount: Int): RewardData() {
    override fun toReward(id: Int): Reward = PointsReward(id, amount)
}