package com.justxraf.skyblockevents.events.custom

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.networkapi.util.Utils.toDate
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.EventType
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
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
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
    var spawnPointsCuboid: MutableMap<Int, Pair<Location, Location>> = mutableMapOf(),
    // For checking the number of entities alive
    // Int = ID, MutableMap<EntityType, UUID>
    var spawnPointsEntities: MutableMap<Int, MutableMap<EntityType, UUID>> = mutableMapOf(),
    var entityTypeForSpawnPoint: MutableMap<Int, EntityType> = mutableMapOf(),

    var regenerativeBlocks: MutableMap<Location, Material> = mutableMapOf()

) : Event(name, uniqueId, eventType, startedAt, endsAt, world, description, spawnLocation, portalLocation, portalCuboid, eventPortalLocation, eventPortalCuboid, questNPCLocation, questNPCUniqueId, quests, playersWhoJoined) {
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

        // get spawnpoint by cuboid - then it gets another map to remove the entities from it and scheduler checks whether there is enough
        // entities spawned in the maps

        /*
        TODO Start a scheduler so it starts to check:
        - Amount of players in the events' world:
          - If there is no players - It should remove all entities,

         */
    }
    // TODO Make a listener to listen to entities spawning naturally and block it.
    private fun checkEntities() {
        val world = spawnLocation.world ?: return
        val deadEntities = world.entities.filter { it.isDead }.map { it.uniqueId }
        deadEntities.forEach { removeEntity(it) }
    }
    private fun removeEntity(uuid: UUID) { // When the entity is killed.
        spawnPointsEntities.forEach { (key, entityMap) ->
            // Remove entries in the inner map that match the UUID
            entityMap.entries.removeIf { it.value == uuid }

            // If the inner map is empty after removals, remove the key from the outer map
            if (entityMap.isEmpty()) {
                spawnPointsEntities.remove(key)
            }
        }
    }
    private fun removeEntities() { // When there are no players
        val world = spawnLocation.world ?: return

        world.entities.forEach {
            if(it is Player) return@forEach
            it.remove()
        }
        spawnPointsEntities.entries.clear()
    }
    override fun end() {
        super.end()
        removeNPC()
        removeHologram()
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