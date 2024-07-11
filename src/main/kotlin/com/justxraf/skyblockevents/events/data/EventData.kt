package com.justxraf.skyblockevents.events.data

import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.custom.NetherEvent
import org.bukkit.Location
import java.util.*

data class EventData(
    var name: String,
    var uniqueId: Int,
    var eventType: EventType,
    var startedAt: Long,
    var endsAt: Long,
    var world: String,
    var description: MutableList<String>,
    var spawnLocation: Location,

    var portalLocation: Location? = null,
    var portalCuboid: Pair<Location, Location>? = null,

    var eventPortalLocation: Location? = null,
    var eventPortalCuboid: Pair<Location, Location>? = null,

    var questNPCLocation: Location? = null,
    var questNPCUniqueId: Int? = null,
    var quests: MutableList<Int>? = null,
    var playersWhoJoined: MutableList<UUID> = mutableListOf()

) {
    fun fromData(): Event {
        return NetherEvent(
            name,
            uniqueId,
            eventType,
            startedAt,
            endsAt,
            world,
            description,
            spawnLocation,
            portalLocation,
            portalCuboid,

            eventPortalLocation,,
            eventPortalCuboid,

            questNPCLocation,
            questNPCUniqueId,
            quests,
            playersWhoJoined
        )

    }
}