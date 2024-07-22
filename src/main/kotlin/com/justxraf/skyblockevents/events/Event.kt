package com.justxraf.skyblockevents.events

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.networkapi.util.Utils.toDate
import com.justxraf.questscore.users.QuestUser
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getEndOfDayMillis
import com.justxraf.skyblockevents.util.isInCuboid
import com.justxraf.skyblockevents.util.pasteSchematic
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.NpcData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

open class Event(
    open var name: String,
    open var uniqueId: Int,

    open var eventType: EventType,
    open var startedAt: Long,

    open var endsAt: Long,
    open var world: String,

    open var description: MutableList<String>,
    open var spawnLocation: Location,

    var requiredLevel: Int = 0,

    open var portalLocation: Location? = null,
    open var portalCuboid: Pair<Location, Location>? = null,

    open var eventPortalLocation: Location? = null,
    open var eventPortalCuboid: Pair<Location, Location>? = null,

    open var questNPCLocation: Location? = null,

    open var quests: MutableList<Int>? = null,
    open val playersWhoJoined: MutableList<UUID> = mutableListOf(),

    // For spawning entities in random places in the cuboid
    open var spawnPointsCuboid: MutableMap<Int, Pair<Location, Location>>? = null,
    open var entityTypeForSpawnPoint: MutableMap<Int, EntityType>? = null,

    open var regenerativeBlocks: MutableMap<Location, Material>? = null,

    /*
    TODO
    Regenerate every 20 seconds after breaking
    Create a listener for plants growth to cancel it whenever there is no players

     */
    open var regenerativePlants: MutableMap<Location, Material>? = null,

    // Live
    open var spawnPointsEntities: MutableMap<Int, MutableMap<UUID, EntityType>>? = mutableMapOf(),
    open var brokenBlocks: MutableMap<Location, Pair<Long, Material>> = mutableMapOf(),
    open var harvestedPlants: MutableMap<Location, Pair<Long, Material>> = mutableMapOf(),
    open var activePlayers: MutableList<UUID> = mutableListOf(),

    private var task: BukkitTask? = null,
    private var activityCheckTask: BukkitTask? = null
) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }


    open fun start() {
        playersWhoJoined.clear()
        clearPlayersQuests()

        startedAt = System.currentTimeMillis()
        endsAt = getEndOfDayMillis(System.currentTimeMillis())

        placePortal()
        placeEventPortal()
        placePortalHologram()
        placeEventPortalHologram()

        reloadNPC()

        runTasks()
        placeRegenerativeBlocks()

        initializeSpawnPointsEntities()

        placeAllRegenerativePlants()
    }
    open fun end() {
        clearPlayersQuests()
        if(portalCuboid != null && portalLocation != null) removePortal(portalLocation!!, portalCuboid!!)
        if(eventPortalCuboid != null && eventPortalLocation != null) removePortal(eventPortalLocation!!, eventPortalCuboid!!)

        removeNPC()

        removeEventPortalHologram()
        removePortalHologram()
        removeQuestNPCHologram()
        removeEntities()

        task?.cancel()
        activityCheckTask?.cancel()

    }

    open fun reload() {
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

        placeAllRegenerativePlants()
    }
    open fun startMessage(): List<String> = emptyList()
    open fun runTasks() {
        task = object : BukkitRunnable() {
            override fun run() {
                checkBlocks()
                checkPlants()

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

    // Checks harvested plants
    private fun checkPlants() {
        if(regenerativePlants.isNullOrEmpty()) regenerativePlants = mutableMapOf()

        plantRegenerativePlants()
    }
    private fun checkMissingRegenerativePlants() {
        if(regenerativePlants.isNullOrEmpty()) regenerativePlants = mutableMapOf()

        regenerativePlants!!.filter { it.key.block.type == Material.AIR && !harvestedPlants.containsKey(it.key) }
            .forEach { (location, material) ->

        }
    }
    private fun placeAllRegenerativePlants() {
        if(regenerativePlants.isNullOrEmpty()) regenerativePlants = mutableMapOf()

        regenerativePlants!!.forEach { (location, material) ->
            val block = location.world?.getBlockAt(location) ?: return@forEach
            block.type = material

            val ageable = block.state as? Ageable ?: return@forEach
            ageable.age = 0
        }
    }
    private fun plantRegenerativePlants() {
        if(harvestedPlants.isEmpty()) return

        harvestedPlants.filter {  System.currentTimeMillis() -  it.value.first < 30000 }.forEach { (location, pair) ->
            val (long, material) = pair

            val block = location.world?.getBlockAt(location) ?: return@forEach
            block.type = material

            val ageable = block.state as? Ageable ?: return@forEach
            ageable.age = 0
        }
    }
    fun harvestRegenerativePlant(location: Location) {
        if(harvestedPlants.isEmpty()) return
        if(regenerativePlants.isNullOrEmpty()) regenerativePlants = mutableMapOf()

        val harvestablePlant = regenerativePlants!![location] ?: return

        harvestedPlants[location] = Pair(System.currentTimeMillis(), harvestablePlant)
    }
    fun canHarvestRegenerativePlant(location: Location): Boolean {
        val block = location.world?.getBlockAt(location) ?: return false

        if(block.blockData !is Ageable) return false
        val ageable = block.blockData as Ageable

        return ageable.age == ageable.maximumAge
    }
    fun isRegenerativePlant(location: Location): Boolean {
        if(regenerativePlants.isNullOrEmpty()) regenerativePlants = mutableMapOf()
        return regenerativePlants!!.containsKey(location)
    }
    fun addRegenerativePlant(location: Location, material: Material) {
        if(regenerativePlants == null) regenerativePlants = mutableMapOf()
        if(regenerativePlants!!.contains(location)) return

        regenerativePlants!![location] = material
    }

    private fun clearPlayersQuests() { // Removes the same quests which were finished previously.
        val availableQuests = quests ?: return

        val usersManager = UsersManager.instance
        val questUsers = Bukkit.getOnlinePlayers().mapNotNull { usersManager.getUser(it.uniqueId) }

        questUsers.forEach { questUser ->
            val keysToRemove =
                questUser.finishedQuests.filter { availableQuests.contains(it.key) && it.value.time < startedAt }
                    .map { it.key }

            keysToRemove.forEach { questUser.finishedQuests.remove(it) }
        }
    }
    private fun removePortal(location: Location, cuboid: Pair<Location, Location>) {
        val pos1 = cuboid.first
        val pos2 = cuboid.second
        val world = BukkitAdapter.adapt(location.world)
        val region =
            CuboidRegion(world, BlockVector3.at(pos1.x, pos1.y, pos1.z), BlockVector3.at(pos2.x, pos2.y, pos2.z))

        WorldEdit.getInstance().newEditSessionBuilder().world(world).build().use {
            val airBlock = BukkitAdapter.adapt(org.bukkit.Material.AIR.createBlockData())
            it.setBlocks(region.faces, airBlock)
        }
    }
    open fun teleport(player: Player) {
        player.teleport(spawnLocation)
        if(!playersWhoJoined.contains(player.uniqueId)) {
            player.sendColoured("&aDołączyłeś do wydarzenia $name pierwszy raz!")
            playersWhoJoined.add(player.uniqueId)
            description.forEach { player.sendColoured(it) }
        } else {
            player.sendColoured("&aPrzeteleportowano do wydarzenia $name!")
        }
    }
    open fun addQuest(id: Int) {
        if(quests == null) quests = mutableListOf()
        if(quests!!.contains(id)) return
        quests!!.add(id)
    }

    // Activity
    private fun checkActivePlayers() {
        val world = spawnLocation.world ?: return
        activePlayers = mutableListOf()

        activePlayers.clear()
        activePlayers = world.entities.filterIsInstance<Player>().map { it.uniqueId }.toMutableList()

        placePortalHologram()
    }

    // Entities
    private fun initializeSpawnPointsEntities() {
        spawnPointsEntities = mutableMapOf()
        if(spawnPointsCuboid == null) spawnPointsCuboid = mutableMapOf()

        spawnPointsCuboid!!.forEach {
            spawnPointsEntities!![it.key] = mutableMapOf()
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

            val worldEntities = world.entities
            val worldEntitiesUUIDs = worldEntities.map { it.uniqueId }
            // Remove entities from the map if they no longer exist in the world
            entities.forEach { uuid ->
                if (!worldEntitiesUUIDs.contains(uuid)) {
                    removeEntity(uuid)
                }
            }

            worldEntities.filter { !entities.contains(it.uniqueId) && it !is Player }.forEach { it.remove() }
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

    // Blocks

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

    // NPC
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


    // Portals
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

    // Holograms

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

    fun restartQuestsFor(questUser: QuestUser) {
        if(quests.isNullOrEmpty()) quests = mutableListOf()
        if(playersWhoJoined.contains(questUser.uniqueId)) return

        questUser.finishedQuests.filter { quests!!.contains(it.key) }
    }

    fun toData() = EventData(
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
}