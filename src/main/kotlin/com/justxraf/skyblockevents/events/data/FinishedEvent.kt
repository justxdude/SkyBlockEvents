package com.justxraf.skyblockevents.events.data

import com.justxraf.skyblockevents.events.data.user.EventUserData
import java.util.UUID

data class FinishedEvent (
    val uuid: UUID,
    var islandsLeaderboard: List<Pair<Int, Int>>,
    var users: Map<UUID, EventUserData>,

    var startedAt: Long,
    var finishedAt: Long
)

// Can retrieve leaderboard from users.
