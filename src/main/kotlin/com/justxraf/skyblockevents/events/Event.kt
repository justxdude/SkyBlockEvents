package com.justxraf.skyblockevents.events

import com.google.gson.annotations.SerializedName
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getEndOfDayMillis
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
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
    open var brokenPlants: MutableMap<Location, Pair<Long, Material>>
) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }

    private fun processPlantHarvest(location: Location) {

    }

    open fun reload() {
        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable { println("hi") }, 200)
    }
    open fun start() {
        playersWhoJoined.clear()
        clearPlayersQuests()

        startedAt = System.currentTimeMillis()
        endsAt = getEndOfDayMillis(System.currentTimeMillis())

    }
    open fun end() {
        clearPlayersQuests()
        if(portalCuboid != null && portalLocation != null) removePortal(portalLocation!!, portalCuboid!!)
        if(eventPortalCuboid != null && eventPortalLocation != null) removePortal(eventPortalLocation!!, eventPortalCuboid!!)
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