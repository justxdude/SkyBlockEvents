package com.justxraf.skyblockevents.events.data

import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.util.isInCuboid
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
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

    var quests: MutableList<Int>? = null,
    var playersWhoJoined: MutableList<UUID> = mutableListOf(),

    var spawnPointsCuboid: MutableMap<Int, Pair<Location, Location>>? = null,

    var entityTypeForSpawnPoint: MutableMap<Int, EntityType>? = null,

    var regenerativeBlocks: MutableMap<Location, Material>? = null,
) {
    fun fromData(): Event {
        val netherEvent = NetherEvent(
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

            eventPortalLocation,
            eventPortalCuboid,

            questNPCLocation,

            quests,
            playersWhoJoined,

            spawnPointsCuboid,
            entityTypeForSpawnPoint,

            regenerativeBlocks
        )
        val questsCopy = quests?.toList()
        println("before adding quests")
        questsCopy?.forEach {
            println("added a quest")
            netherEvent.addQuest(it)
        }
        println("after adding quests")

        return netherEvent
    }
    fun getSpawnPointIdAt(location: Location): Int? {
        if(spawnPointsCuboid == null) return null
        return spawnPointsCuboid?.entries?.firstNotNullOfOrNull { (key, pair) ->
            if (isInCuboid(location, pair.first, pair.second)) key else null
        }
    }
    fun addQuest(id: Int) {
        if(quests == null) quests = mutableListOf()
        if(quests!!.contains(id)) return
        quests!!.add(id)
    }
}