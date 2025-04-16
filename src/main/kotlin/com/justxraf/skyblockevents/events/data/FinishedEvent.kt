package com.justxraf.skyblockevents.events.data

import java.util.UUID

class FinishedEvent (
    val uuid: UUID,
    var islandsLeaderboard: List<Pair<Int, Int>>,

    var playersLeaderboard: List<Pair<UUID, Int>>,
    var playersWhoJoined: List<UUID>,

    var startedAt: Long,
    var finishedAt: Long,


) {
}