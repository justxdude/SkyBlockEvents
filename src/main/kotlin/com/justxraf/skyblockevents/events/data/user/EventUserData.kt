package com.justxraf.skyblockevents.events.data.user

import com.justxraf.skyblockevents.users.EventUser
import java.util.UUID

data class EventUserData(
    val uniqueId: UUID,
    val points: Int,

    val mobsKilled: Int,
    val blocksMined: Int,
    val deaths: Int,

    val questsFinished: MutableList<Int>,
    val isActive: Boolean,

    val lastCache: Long,
) {
    fun toEventUser(): EventUser = EventUser(uniqueId, points, mobsKilled, blocksMined, deaths, questsFinished, isActive, lastCache)
}