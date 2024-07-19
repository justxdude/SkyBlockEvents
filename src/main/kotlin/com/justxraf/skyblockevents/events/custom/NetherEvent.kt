package com.justxraf.skyblockevents.events.custom

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.networkapi.util.Utils.toDate
import com.justxraf.questscore.component.ComponentsManager
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

    // Live event
    private var spawnPointsEntities: MutableMap<Int, MutableMap<UUID, EntityType>>? = mutableMapOf(),
    private var brokenBlocks: MutableMap<Location, Pair<Long, Material>> = mutableMapOf(),
    private var activePlayers: MutableList<UUID> = mutableListOf(),
    private var task: BukkitTask? = null,
    private var activityCheckTask: BukkitTask? = null

    ) : Event(name, uniqueId, eventType, startedAt, endsAt, world, description, spawnLocation, portalLocation, portalCuboid, eventPortalLocation, eventPortalCuboid, questNPCLocation, quests, playersWhoJoined) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }


    /*

    TODO:
    - Add regenerative plants:
     - Checks the plant every X seconds (10 to 15 seconds)
     - If the plant doesn't exist - it should plant a young one.
    TODO: Move everything to Event class

     */


    private fun checkActivePlayers() {
        val world = spawnLocation.world ?: return
        activePlayers = mutableListOf()

        activePlayers.clear()
        activePlayers = world.entities.filterIsInstance<Player>().map { it.uniqueId }.toMutableList()

        placePortalHologram()
    }
    private fun initializeSpawnPointsEntities() {
        spawnPointsEntities = mutableMapOf()
        if(spawnPointsCuboid == null) spawnPointsCuboid = mutableMapOf()

        spawnPointsCuboid!!.forEach {
            spawnPointsEntities!![it.key] = mutableMapOf()
        }
    }

    override fun reload() {
        runTasks()
        Bukkit.getScheduler().runTaskLater(components.plugin, Runnable {
            placeRegenerativeBlocks()
        }, 100)

        initializeSpawnPointsEntities()
        checkActivePlayers()

        placePortal()
        placeEventPortal()
        placePortalHologram()
        placeEventPortalHologram()

        reloadNPC()

        removeQuestNPCHologram()
        createQuestNPCsHologram()
        removeEntities()
    }
    private fun runTasks() {
        task = object : BukkitRunnable() {
            override fun run() {
                checkBlocks()

                val playersInTheEvent = Bukkit.getOnlinePlayers().filter { it.world == spawnLocation.world }.size
                if(playersInTheEvent == 0) {
                    removeEntities()
                    return
                }
                checkEntities()
            }
        }.runTaskTimer(components.plugin, 0, 20 * 5) // Check every 5 seconds.
        activityCheckTask = object : BukkitRunnable() {
            override fun run() {
                checkActivePlayers()
            }
        }.runTaskTimer(components.plugin, 0, 20 * 10) // Check every 10 seconds.

    }
    override fun start() {
        super.start()

        val portalLocation = portalLocation!!

        placePortal()
        placeEventPortal()
        placePortalHologram()
        placeEventPortalHologram()

        Bukkit.getOnlinePlayers().forEach {
            it.sendColoured("&aWydarzenie Nether rozpoczęło się! Na spawnie w lokalizacji" +
                    " ${portalLocation.x}, ${portalLocation.y}, ${portalLocation.z} " +
                    "(XYZ) pojawił się portal do którego możesz wejść i dołączyć do wydarzenia!")
        }
        reloadNPC()

        runTasks()
        placeRegenerativeBlocks()

        initializeSpawnPointsEntities()
    }
    override fun end() {
        super.end()
        removeNPC()

        removeEventPortalHologram()
        removePortalHologram()
        removeQuestNPCHologram()
        removeEntities()

        task?.cancel()
        activityCheckTask?.cancel()
    }
    private fun placeRegenerativeBlocks() {
        if(regenerativeBlocks == null) regenerativeBlocks = mutableMapOf()
        val world = spawnLocation.world ?: return

        regenerativeBlocks?.forEach { (key, value) ->
            world.getBlockAt(key).type = value
        }
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
        try {
            val currentTime = System.currentTimeMillis()
            val world = spawnLocation.world ?: return

            brokenBlocks.filter { (_, value) -> (currentTime - value.first) > 6000 }
                .forEach { (key, value) ->
                    world.getBlockAt(key).type = value.second
                    brokenBlocks.remove(key)
                }
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun isEventEntity(uuid: UUID): Boolean {
        try {
            if (spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
            return spawnPointsEntities!!.any { (_, value) -> value.keys.contains(uuid) }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    private fun removeEntities() {
        try {
            val world = spawnLocation.world ?: return

            world.entities.forEach {
                if (it is Player) return@forEach
                it.remove()
            }

            if (spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
            spawnPointsEntities?.clear()
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun checkEntities() {
        try {
            val world = spawnLocation.world ?: return

            if (spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
            val entities = spawnPointsEntities!!.flatMap { it.value.keys }.toMutableList()
            val worldEntities = world.entities.map { it.uniqueId }

            // Remove entities from the map if they no longer exist in the world
            entities.forEach { uuid ->
                if (!worldEntities.contains(uuid)) {
                    removeEntity(uuid)
                }
            }
            shouldSpawnEntity()
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeEntity(uuid: UUID) {
        try {
            if (spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
            spawnPointsEntities?.let { entitiesMap ->
                entitiesMap.forEach { (_, entityMap) ->
                    entityMap.remove(uuid)
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shouldSpawnEntity() {
        try {
            if (spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()

            if (spawnPointsEntities!!.isEmpty())
                spawnPointsCuboid!!.forEach {
                    spawnPointsEntities!![it.key] = mutableMapOf()
                }

            spawnPointsEntities?.let { entitiesMap ->
                entitiesMap.forEach { (mapID, entityMap) ->
                    if (entityMap.size < 5) {
                        spawnEntityInCuboid(mapID)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun spawnEntityInCuboid(mapID: Int) {
        try {
            if (entityTypeForSpawnPoint == null) entityTypeForSpawnPoint = mutableMapOf()

            val entityType = entityTypeForSpawnPoint!![mapID] ?: return // EntityType
            if (spawnPointsCuboid == null) spawnPointsCuboid = mutableMapOf()

            val cuboid = spawnPointsCuboid!![mapID] ?: return // Pair <Location, Location>
            val world = spawnLocation.world ?: return

            val location = getRandomLocationWithinCuboid(cuboid)
            val entity = world.spawnEntity(location, entityType) as Mob

            if (spawnPointsEntities == null) spawnPointsEntities = mutableMapOf()
            if (spawnPointsEntities!![mapID] == null) {
                spawnPointsEntities!![mapID] = mutableMapOf()
            }

            spawnPointsEntities!![mapID]?.put(entity.uniqueId, entityType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRandomLocationWithinCuboid(cuboid: Pair<Location, Location>): Location {
        val random = Random()

        val (loc1, loc2) = cuboid
        val world = loc1.world

        val minX = minOf(loc1.x, loc2.x)
        val maxX = maxOf(loc1.x, loc2.x)
        val minZ = minOf(loc1.z, loc2.z)
        val maxZ = maxOf(loc1.z, loc2.z)

        val randomX = if (minX == maxX) minX else random.nextDouble(minX, maxX)
        val randomZ = if (minZ == maxZ) minZ else random.nextDouble(minZ, maxZ)
        val randomY = loc1.y

        return Location(world, randomX, randomY, randomZ)
    }


    private fun getEntityCuboid(location: Location): Int? {
        if(spawnPointsCuboid == null) {
            spawnPointsCuboid = mutableMapOf()
            return null
        }
        return spawnPointsCuboid?.entries?.firstNotNullOfOrNull { (key, pair) ->
            if (isInCuboid(location, pair.first, pair.second)) key else null
        }
    }
    private fun spawnQuestNPC() {
        try {
            if (questNPCLocation == null) return

            val npcData = NpcData("${uniqueId}_event_npc", null, questNPCLocation)

            npcData.isGlowing = true
            npcData.isShowInTab = false

            npcData.isCollidable = true
            npcData.isTurnToPlayer = false

            npcData.setInteractionCooldown(5F)

            npcData.setDisplayName("<empty>")

            val npc = FancyNpcsPlugin.get().npcAdapter.apply(npcData)

            npc.create()
            npc.spawnForAll()
            npc.updateForAll()

            FancyNpcsPlugin.get().npcManager.registerNpc(npc)
            createQuestNPCsHologram()

        } catch (e: Exception) {
        e.printStackTrace()
    }
    }
    private fun createQuestNPCsHologram() {
        try {
            val npcManager = FancyNpcsPlugin.get().npcManager
            val npc = npcManager.getNpc("${uniqueId}_event_npc") ?: return

            val hologramLocation = npc.data.location.clone().add(.0, npc.eyeHeight + 0.5, .0)

            val description = listOf("&cDiabeł", "&7Kliknij aby rozmawiać")
            val hologramManager = FancyHologramsPlugin.get().hologramManager

            val hologram = hologramManager.getHologram("${uniqueId}_event_npc_hologram").orElse(null)
            if (hologram != null) return

            val hologramData = TextHologramData("${uniqueId}_event_npc_hologram", hologramLocation)
            hologramData.setText(description)
            hologramData.setLinkedNpcName("${uniqueId}_event_npc_hologram")

            hologramManager.addHologram(hologramManager.create(hologramData))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun removeNPC() {
        try {
            if(questNPCLocation == null) return

            val npc = FancyNpcsPlugin.get().npcManager.getNpc("${uniqueId}_event_npc")
            if(npc == null) return

            npc.removeForAll()
            npc.updateForAll()
            FancyNpcsPlugin.get().npcManager.removeNpc(npc)
            FancyNpcsPlugin.get().npcManager.reloadNpcs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun reloadNPC() {
        try {
            FancyNpcsPlugin.get().npcManager.reloadNpcs()
            val npc = FancyNpcsPlugin.get().npcManager.getNpc("${uniqueId}_event_npc")

            if (npc == null) {
                spawnQuestNPC()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun placePortal() {
        try {
         portalCuboid = pasteSchematic(portalLocation!!, "NetherPortal")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun placeEventPortal() {
        try {
        eventPortalCuboid = pasteSchematic(eventPortalLocation!!, "EventNetherPortal")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun removeQuestNPCHologram() {
        try {
            val hologramManager = FancyHologramsPlugin.get().hologramManager

            val hologram = hologramManager.getHologram("${uniqueId}_event_npc_hologram").orElse(null)
            if (hologram != null) hologramManager.removeHologram(hologram)
        } catch (e: Exception) {
        e.printStackTrace()
    }
    }
    // Holograms
    private fun removePortalHologram() {
        try {
            val hologramManager = FancyHologramsPlugin.get().hologramManager

            val hologram = hologramManager.getHologram("${uniqueId}_portal_hologram").orElse(null)
            if (hologram != null) hologramManager.removeHologram(hologram)
        } catch (e: Exception) {
        e.printStackTrace()
    }
    }
    private fun placePortalHologram() {
        try {
            removePortalHologram()

            val centre = portalLocation ?: return
            val hologramLocation = Location(centre.world, centre.x + 1.3, centre.y + 4, centre.z - 0.2)
            val lines = listOf(
                "&a&lWydarzenie $name",
                "&aLiczba aktywnych graczy: ${activePlayers.size}",
                "&a${if (playersWhoJoined.size < 2) "Dołączył łącznie tylko jeden gracz" else "Dołączyło łącznie &7${playersWhoJoined.size} graczy&a."}",
                "&7Kończy się o ${endsAt.toDate()}",
                "",
                "&7Wejdź w portal aby dołączyć!"
            )
            val hologramManager = FancyHologramsPlugin.get().hologramManager
            val hologramData = TextHologramData("${uniqueId}_portal_hologram", hologramLocation)
            hologramData.setText(lines)
            hologramData.setBillboard(Display.Billboard.CENTER)

            hologramManager.addHologram(hologramManager.create(hologramData))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun removeEventPortalHologram() {
        try {
        val hologramManager = FancyHologramsPlugin.get().hologramManager

        val hologram = hologramManager.getHologram("${uniqueId}_event_portal_hologram").orElse(null)
        if(hologram != null) hologramManager.removeHologram(hologram)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun placeEventPortalHologram() {
        try {
            removeEventPortalHologram()

            val centre = eventPortalLocation ?: return
            val hologramLocation = Location(centre.world, centre.x, centre.y + 4, centre.z + .5)
            val lines = listOf("&aTeleportuj do", "&7Spawn")

            val hologramManager = FancyHologramsPlugin.get().hologramManager
            val hologramData = TextHologramData("${uniqueId}_event_portal_hologram", hologramLocation)

            hologramData.setText(lines)
            hologramData.setBillboard(Display.Billboard.CENTER)

            hologramManager.addHologram(hologramManager.create(hologramData))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}