package com.justxraf.skyblockevents.events.custom

import com.justxdude.islandcore.utils.toLocationString
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.networkapi.util.Utils.toDate
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.questscore.component.ComponentsManager
import com.justxraf.questscore.utils.sendMessages
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.util.isInCuboid
import com.justxraf.skyblockevents.util.pasteSchematic
import com.justxraf.skyblockevents.util.plugin
import com.justxraf.skyblockevents.util.pushPlayerIfClose
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import de.oliver.fancynpcs.api.NpcData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*


class NetherEvent(
    override var name: String,
    override var uniqueId: Int,
    override var eventType: EventType,
    override var startedAt: Long,
    override var endsAt: Long,
    override var world: String,
    override var description: MutableList<String>,
    override var spawnLocation: Location,

    override var portalLocation: Location? = null,
    override var portalCuboid: Pair<Location, Location>? = null,

    override var eventPortalLocation: Location? = null,
    override var eventPortalCuboid: Pair<Location, Location>? = null,

    override var questNPCLocation: Location? = null,
    override var quests: MutableList<Int>? = null,
    override var playersWhoJoined: MutableList<UUID> = mutableListOf(),

    // For spawning entities in random places in the cuboid
    override var spawnPointsCuboid: MutableMap<Int, Pair<Location, Location>>? = mutableMapOf(),
    // For checking the number of entities alive
    // Int = ID, MutableMap<EntityType, UUID>
    override var entityTypeForSpawnPoint: MutableMap<Int, EntityType>? = mutableMapOf(),

    override var regenerativeBlocks: MutableMap<Location, Material>? = mutableMapOf(),
    override var regenerativePlants: MutableMap<Location, Material>? = mutableMapOf(),

    // Live event
    override var harvestedPlants: MutableMap<Location, Pair<Long, Material>> = mutableMapOf(),
    override var spawnPointsEntities: MutableMap<Int, MutableMap<UUID, EntityType>>? = mutableMapOf(),
    override var brokenBlocks: MutableMap<Location, Pair<Long, Material>> = mutableMapOf(),
    override var activePlayers: MutableList<UUID> = mutableListOf(),

    ): Event(name,
    uniqueId, eventType,
    startedAt, endsAt,
    world, description,
    spawnLocation, 10, portalLocation,
    portalCuboid, eventPortalLocation,
    eventPortalCuboid, questNPCLocation,
    quests, playersWhoJoined,
    spawnPointsCuboid, entityTypeForSpawnPoint,
    regenerativeBlocks, regenerativePlants,
    spawnPointsEntities, brokenBlocks,
    harvestedPlants, activePlayers) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }


    /*

    TODO:
    - Add regenerative plants:
     - Checks the plant every X seconds (10 to 15 seconds)
     - If the plant doesn't exist - it should plant a young one.
    TODO: Move everything to Event class

     */

    override fun reload() {
        super.reload()
    }

    override fun start() {
        super.start()

        Bukkit.getOnlinePlayers().sendMessages(
            *startMessage().toTypedArray()
        ) {
            val user = it.asUser()
            user != null && user.level >= requiredLevel
        }
    }

    override fun startMessage(): List<String> = listOf(
        "&9&m-".repeat(35),
        "&aWydarzenie $name rozpoczęło się!",
        "&aNa spawnie (${portalLocation?.toLocationString()} XYZ) pojawił się portal",
        "&aPrzez który możesz dołączyć do wydarzenia!",
        "&9&m-".repeat(35),
    )
}