package com.justxraf.skyblockevents.events.entities

import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.util.isInCuboid
import io.lumine.mythic.core.mobs.ActiveMob
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EventEntitiesHandler(
    val cuboids: ConcurrentHashMap<Int, EventEntityCuboid> = ConcurrentHashMap()
) {
    @Transient
    lateinit var entities: ConcurrentHashMap<UUID, EventEntityCuboid>
    @Transient
    lateinit var cuboidLocations: ConcurrentHashMap<Pair<Location, Location>, EventEntityCuboid>
    @Transient
    lateinit var event: Event
    @Transient
    var isSetup: Boolean = false

    fun setup(liveEvent: Event) {
        event = liveEvent

        entities = ConcurrentHashMap()
        setupCuboids()

        isSetup = true
    }
    fun reload(liveEvent: Event) {
        if(!isSetup) {
            setup(liveEvent)
            return
        }
        reloadCuboids()
        removeBukkitEntities()
    }
    fun stop() {
        stopCuboids()
    }
    //
    private fun reloadCuboids() {
        cuboids.values.forEach { it.reload(this) }
    }
    private fun setupCuboids() {
        cuboids.values.forEach { it.setup(this) }
        setupCuboidLocations()
    }
    private fun stopCuboids() {
        cuboids.values.forEach { it.stop() }
    }
    private fun setupCuboidLocations() {
        cuboidLocations = cuboids.values.associateByTo(ConcurrentHashMap()) { it.cuboid }
    }
    //
//    fun isInEntityCuboid(location: Location): Boolean =
//        cuboidLocations.any { (cuboid, _) -> location.isInCuboid(cuboid) }

    //
    fun removeCuboidBy(location: Location): Boolean {
        val cuboid = cuboidLocations.entries.firstOrNull { location.isInCuboid(it.key) }?.value ?: return false

        removeCuboid(cuboid)
        return true
    }
    fun removeCuboid(cuboid: EventEntityCuboid) {
        cuboid.stop()
        cuboids.remove(cuboid.id)

        entities.values.removeIf { it.id == cuboid.id }
        cuboidLocations.remove(cuboid.cuboid)
    }
    //
//    fun removeEntities() {
//        val mobManager = MythicBukkit.inst().mobManager
//        entities.map { mobManager.getActiveMob(it.key).getOrNull() }
//            .filterNotNull()
//            .forEach {
//                removeEntity(it)
//            }
//        entities.clear()
//    }
    fun removeEntity(activeMob: ActiveMob) {
        val cuboid = getCuboidBy(activeMob.uniqueId) ?: return

        cuboid.removeEntity(activeMob)
        entities.remove(activeMob.uniqueId)
    }
    private fun getCuboidBy(entityUuid: UUID) =
        entities.entries.firstOrNull { it.key == entityUuid }?.value

    private fun removeBukkitEntities() {
        event.spawnLocation.world?.entities
            ?.filter { it !is Player }
            ?.forEach {
                it.remove()
            }
    }
    fun addEntity(uniqueId: UUID, cuboid: EventEntityCuboid) {
        entities.putIfAbsent(uniqueId, cuboid)
    }
    fun createCuboid(cuboid: EventEntityCuboid) {
        cuboids.put(cuboid.id, cuboid)
        cuboid.setup(this)
    }
}