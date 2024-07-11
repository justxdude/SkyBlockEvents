package com.justxraf.skyblockevents.events

import com.google.gson.annotations.SerializedName
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getEndOfDayMillis
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
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
    open var questNPCUniqueId: Int? = null,
    open var quests: MutableList<Int>? = null,
    open val playersWhoJoined: MutableList<UUID> = mutableListOf()
) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }

    open fun start() {
        playersWhoJoined.clear()
        clearPlayersQuests()

        startedAt = System.currentTimeMillis()
        endsAt = getEndOfDayMillis(System.currentTimeMillis())

    }
    open fun end() {
        clearPlayersQuests()
        removePortal()
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
    private fun removePortal() {
        if(portalCuboid == null) return

        val pos1 = portalCuboid?.first!!
        val pos2 = portalCuboid?.second!!
        val world = BukkitAdapter.adapt(portalLocation!!.world)
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
        questNPCUniqueId,
        quests,
        playersWhoJoined
    )
}