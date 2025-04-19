package com.justxraf.skyblockevents.events.data.user

import com.justxraf.skyblockevents.users.EventUser
import java.util.UUID

data class EventUserData(
    val uniqueId: UUID,
    var points: Int,

    var mobsKilled: Int,
    var blocksMined: Int,

    var questsFinished: MutableList<Int>,
    var isActive: Boolean,

    var lastCache: Long,
) {
    fun toEventUser(): EventUser = EventUser(uniqueId, points, mobsKilled, blocksMined, questsFinished, isActive, lastCache)
}