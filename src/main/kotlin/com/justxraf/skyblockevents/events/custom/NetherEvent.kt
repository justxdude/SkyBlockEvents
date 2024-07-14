package com.justxraf.skyblockevents.events.custom

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.networkapi.util.Utils.toDate
import com.justxraf.questscore.component.ComponentsManager
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.util.isInPortal
import com.justxraf.skyblockevents.util.pasteSchematic
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import eu.decentsoftware.holograms.api.DHAPI
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
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
    override var questNPCUniqueId: Int? = null,
    override var quests: MutableList<Int>? = null,
    override var playersWhoJoined: MutableList<UUID> = mutableListOf(),

    // For spawning entities in random places in the cuboid
    override var spawnPointsCuboid: MutableMap<Int, Pair<Location, Location>>? = mutableMapOf(),
    // For checking the number of entities alive
    // Int = ID, MutableMap<EntityType, UUID>
    var spawnPointsEntities: MutableMap<Int, MutableMap<UUID, EntityType>>? = mutableMapOf(),
    override var entityTypeForSpawnPoint: MutableMap<Int, EntityType>? = mutableMapOf(),

    override var regenerativeBlocks: MutableMap<Location, Material>? = mutableMapOf(),

    // Live event
    var brokenBlocks: MutableMap<Location, Pair<Long, Material>> = mutableMapOf(),
    private var task: BukkitTask? = null,

    ) : Event(name, uniqueId, eventType, startedAt, endsAt, world, description, spawnLocation, portalLocation, portalCuboid, eventPortalLocation, eventPortalCuboid, questNPCLocation, questNPCUniqueId, quests, playersWhoJoined) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }

    override fun start() {
        super.start()

        val portalLocation = portalLocation!!
        placePortal()
        placeHologram()

        Bukkit.getOnlinePlayers().forEach {
            it.sendColoured("&aWydarzenie Nether rozpoczęło się! Na spawnie w lokalizacji" +
                    " ${portalLocation.x}, ${portalLocation.y}, ${portalLocation.z} " +
                    "(XYZ) pojawił się portal do którego możesz wejść i dołączyć do wydarzenia!")
        }
        spawnQuestNPC()

        task = object : BukkitRunnable() {
            override fun run() {
                val playersInTheEvent = Bukkit.getOnlinePlayers().filter { it.world == spawnLocation.world }.size
                if(playersInTheEvent == 0) {
                    removeEntities()
                    return
                }
                checkEntities()

            }
        }.runTaskTimer(components.plugin, 0, 20 * 5) // Check every 5 seconds.
        Bukkit.getScheduler().runTaskTimer(components.plugin, Runnable {
            checkBlocks()
        }, 20, 20)
        /*

          TODO Checks: On entity death (remove from the map etc.), on entity void fall, on entity damage another entity
          TODO:
           Do the minerals "respawn"
           mineral is mined > becomes a bedrock for 5 seconds and changes back to the original form.
           make a scheduler to check for broken blocks.
         */
    }
    override fun end() {
        super.end()
        removeNPC()
        removeHologram()
        task?.cancel()
    }
    fun breakRegenerativeBlockAt(location: Location) {
        if(regenerativeBlocks == null) regenerativeBlocks = mutableMapOf()
        val material = regenerativeBlocks!![location] ?: return
        if(brokenBlocks == null) brokenBlocks = mutableMapOf()
        brokenBlocks[location] = Pair(System.currentTimeMillis(), material)
    }
    fun isRegenerativeBlock(location: Location): Boolean {
        if(regenerativeBlocks == null) regenerativeBlocks = mutableMapOf()
        return regenerativeBlocks!!.containsKey(location)
    }
    private fun checkBlocks() {
        val currentTime = System.currentTimeMillis()
        val world = spawnLocation.world ?: return
        brokenBlocks.filter { (_, value) -> currentTime - value.first > 6000 && value.second == Material.AIR }
            .forEach { (key, value) ->
                world.getBlockAt(key).type = value.second
            }
    }
    private fun checkEntities() {
        val world = spawnLocation.world ?: return
        val deadEntities = world.entities.filter { it.isDead }.map { it.uniqueId }
        deadEntities.forEach { removeEntity(it) }

        shouldSpawnEntity()
    }
    fun removeEntity(uuid: UUID) { // When the entity is killed.
        if(spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
        spawnPointsEntities?.let {
            it.forEach { (key, entityMap) ->
                entityMap.entries.removeIf { it.key == uuid }
                if (entityMap.isEmpty()) {
                    spawnPointsEntities?.entries?.clear()
                }
            }
        }
    }
    private fun removeEntities() { // When there are no players
        val world = spawnLocation.world ?: return

        world.entities.forEach {
            if(it is Player) return@forEach
            it.remove()
        }
        if(spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
        spawnPointsEntities?.entries?.clear()
    }
    private fun spawnEntityInCuboid(mapID: Int) {
        if(entityTypeForSpawnPoint == null) entityTypeForSpawnPoint = mutableMapOf()
        val entityType = entityTypeForSpawnPoint!![mapID] ?: return // EntityType
        if(spawnPointsCuboid == null) spawnPointsCuboid = mutableMapOf()
        val cuboid = spawnPointsCuboid!![mapID] ?: return // Pair <Location, Location>

        val world = spawnLocation.world ?: return
        val entity = world.spawnEntity(getRandomLocationWithinCuboid(cuboid), entityType)

        if(spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
        if (spawnPointsEntities!![mapID] == null) {
            spawnPointsEntities!![mapID] = mutableMapOf()
        }
        spawnPointsEntities!![mapID]?.set(entity.uniqueId, entityType)
    }
    private fun shouldSpawnEntity() {
        if(spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
        spawnPointsEntities?.let {
            it.forEach { (key, entityMap) ->
                if (entityMap.size > 5) return@forEach
                spawnEntityInCuboid(key)
            }
        }
    }
    private fun getRandomLocationWithinCuboid(cuboid: Pair<Location, Location>): Location {
        val (loc1, loc2) = cuboid
        val world = loc1.world

        val minX = minOf(loc1.x, loc2.x)
        val maxX = maxOf(loc1.x, loc2.x)
        val minZ = minOf(loc1.z, loc2.z)
        val maxZ = maxOf(loc1.z, loc2.z)

        val randomX = Random().nextDouble(minX, maxX)
        val randomZ = Random().nextDouble(minZ, maxZ)
        val randomY = loc1.y

        return Location(world, randomX, randomY, randomZ)
    }
    private fun getEntityCuboid(location: Location): Int? {
        if(spawnPointsCuboid == null) {
            spawnPointsCuboid = mutableMapOf()
            return null
        }
        return spawnPointsCuboid?.entries?.firstNotNullOfOrNull { (key, pair) ->
            if (isInPortal(location, pair.first, pair.second)) key else null
        }
    }
    private fun spawnQuestNPC() {
        val npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "&cDiabeł")
        npc.spawn(questNPCLocation)
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false)

        npc.isProtected = true
        npc.isFlyable = false

        questNPCUniqueId = npc.id

        val description = listOf("&cDiabeł", "&7Kliknij aby rozmawiać")

        val hologramLocation = Location(npc.storedLocation.world,
            npc.storedLocation.x,
            npc.storedLocation.y + 2.0 + (description.size * 0.4) - 0.5,
            npc.storedLocation.z
            )
        DHAPI.getHologram("${npc.id}")?.destroy()

        DHAPI.createHologram("${npc.id}", hologramLocation, true, description)
    }
    private fun removeNPC() {
        if(questNPCUniqueId == null) return
        CitizensAPI.getNPCRegistry().getById(questNPCUniqueId!!).destroy()
        DHAPI.removeHologram("$questNPCUniqueId")
    }
    private fun placePortal() {
         portalCuboid = pasteSchematic(portalLocation!!, "NetherPortal")
        // holograms TODO Make events for each action so holograms can be changed.
    }
    private fun placeHologram() {
        val centre = portalLocation ?: return
        val hologramLocation = Location(centre.world, centre.x + 1.3, centre.y + 4, centre.z + .5)

        val lines = listOf(
            "&a&lWydarzenie $name",
            "&aLiczba graczy: &6${playersWhoJoined.size}",
            "&7Kończy się o ${endsAt.toDate()}",
        )

        DHAPI.getHologram(uniqueId.toString())?.destroy()
        DHAPI.createHologram(uniqueId.toString(), hologramLocation, true, lines).lock
    }
    private fun removeHologram() {
        DHAPI.getHologram(uniqueId.toString())?.destroy()
    }
}