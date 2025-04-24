package com.justxraf.skyblockevents.events.data.active

import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.user.EventUserData
import com.justxraf.skyblockevents.events.entities.EventEntitiesHandler
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterialsHandler
import com.justxraf.skyblockevents.users.EventUserHandler
import com.justxraf.skyblockevents.users.points.PointsHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class ActiveEventData(
    val uniqueId: Int,
    val type: EventType,

    val startedAt: Long,
    val endsAt: Long,

    val eventUsers: ConcurrentHashMap<UUID, EventUserData>,
    val uuid: UUID,
    ) {


    fun toEvent(): Event {
        val eventsManager = EventsManager.instance
        val eventData = eventsManager.events[uniqueId]!!

        val event =
            Event(
                eventData.name,
                uniqueId,
                type,
                startedAt,
                endsAt,
                eventData.description,
                eventData.spawnLocation,
                RegenerativeMaterialsHandler(eventData.regenerativeMaterials ?: mutableListOf()),
                EventEntitiesHandler(eventData.eventEntityCuboids ?: ConcurrentHashMap()),
                EventUserHandler(PointsHandler(), eventUsers.entries.associateTo(ConcurrentHashMap()) { it.key to it.value.toEventUser() }),
                uuid,
                eventData.requiredLevel,
                eventData.portals,
                eventData.spawnRegion,
                eventData.questNPCLocation,
                eventData.quests,
            )

        return event
    }
}