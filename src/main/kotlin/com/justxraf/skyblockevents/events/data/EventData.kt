package com.justxraf.skyblockevents.events.data

import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.data.user.EventUserData
import com.justxraf.skyblockevents.events.entities.EventEntitiesHandler
import com.justxraf.skyblockevents.events.entities.EventEntityCuboid
import com.justxraf.skyblockevents.users.points.PointsHandler
import com.justxraf.skyblockevents.events.portals.EventPortal
import com.justxraf.skyblockevents.events.portals.EventPortalType
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterial
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterialsHandler
import com.justxraf.skyblockevents.users.EventUserHandler
import com.justxraf.skyblockevents.util.isInCuboid
import org.bukkit.Location
import org.bukkit.Material
import java.time.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class EventData(
    var name: String,
    var uniqueId: Int,

    var type: EventType,
    var description: MutableList<String>,

    var spawnLocation: Location,
    var requiredLevel: Int = 0,

    var portals: ConcurrentHashMap<EventPortalType, EventPortal>? = null,
    var spawnRegion: Pair<Location, Location>? = null,

    var questNPCLocation: Location? = null,
    var quests: MutableList<Int>? = null,

    var eventEntityCuboids: ConcurrentHashMap<Int, EventEntityCuboid>? = null,
    var regenerativeMaterials: MutableList<RegenerativeMaterial>? = null,

    ) {

    // This function creates a new event for CurrentEvent.
    fun fromData(): Event {
        val regenerativeBlocksManager = RegenerativeMaterialsHandler(regenerativeMaterials ?: mutableListOf())
        val eventEntitiesHandler = EventEntitiesHandler(eventEntityCuboids ?: ConcurrentHashMap())

        val eventUserHandler = EventUserHandler(PointsHandler(), ConcurrentHashMap())

        val zone = ZoneId.of("Europe/Berlin")
        val today = LocalDate.now(zone)
        val endOfDay = LocalTime.of(23, 59, 59)
        val zonedTime = ZonedDateTime.of(today, endOfDay, zone)

        val endsAt = zonedTime.toInstant().toEpochMilli()

        val event = Event(
            name,
            uniqueId,
            type,
            System.currentTimeMillis(),
            endsAt,
            description,
            spawnLocation,
            regenerativeBlocksManager,
            eventEntitiesHandler,
            eventUserHandler
        )
        val questsCopy = quests?.toList()

        questsCopy?.forEach {
            event.addQuest(it)
        }
        portals?.forEach {
            it.value.event = event
        }

        return event
    }

    // Entity Cuboids

    fun isInEntityCuboid(location: Location): Boolean {
        if(eventEntityCuboids.isNullOrEmpty()) return false
        return eventEntityCuboids!!.entries.any { (_, value) ->
            location.isInCuboid(value.cuboid)
        }
    }
    fun removeEntityCuboidBy(location: Location) {
        if(eventEntityCuboids.isNullOrEmpty()) return
        eventEntityCuboids!!.entries.removeIf { (_, value) ->
            location.isInCuboid(value.cuboid)
        }
    }
    fun getEntityCuboidBy(location: Location): Int? {
        if(eventEntityCuboids.isNullOrEmpty()) return null
        return eventEntityCuboids!!.firstNotNullOfOrNull { (key, it) ->
            if(location.isInCuboid(it.cuboid)) key else null
        }
    }


    fun addQuest(id: Int) {
        if(quests == null) quests = mutableListOf()
        if(quests!!.contains(id)) return
        quests!!.add(id)
    }
    fun isRegenerativeMaterial(material: Material): Boolean {
        if(regenerativeMaterials == null) regenerativeMaterials = mutableListOf()
        return regenerativeMaterials!!.any { it.material == material }
    }
    fun addRegenerativeMaterial(
        material: Material,
        isHarvestable: Boolean,
    ) {
        if(regenerativeMaterials == null) regenerativeMaterials = mutableListOf()
        if(isRegenerativeMaterial(material)) return

        regenerativeMaterials!!.add(RegenerativeMaterial(material, isHarvestable))
    }
    fun removeRegenerativeMaterial(material: Material) {
        if(regenerativeMaterials.isNullOrEmpty()) return
        regenerativeMaterials?.removeIf { it.material == material }
    }
}