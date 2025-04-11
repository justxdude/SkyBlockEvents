package com.justxraf.skyblockevents.events.event

import com.github.supergluelib.foundation.round
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventsManager
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import io.lumine.mythic.core.mobs.ActiveMob
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

class EventEntityCuboid(
    val id: Int,
    private var entityType: String,
    var cuboid: Pair<Location, Location>,
    private var level: Int,
    private var limit: Int, // How many entities should be spawned.
) {
    // live
    @Transient lateinit var eventEntitiesManager: EventEntitiesManager
    @Transient lateinit var entities: MutableList<ActiveMob>
    @Transient lateinit var task: BukkitTask
    @Transient var isSetup: Boolean = false
    @Transient var spawnDelay: Long = 0
    @Transient var lastSpawn: Long = 0

    fun isAnyPlayerNearby(): Boolean {
        val location = cuboid.first
        val visibilityDistanceSquared = 30 * 30

        val event = eventEntitiesManager.event

        return event.activePlayers.any { (uuid, player) ->
            val playerLocation = player.player.location
            val dx = location.x.round(0) - playerLocation.x.round(0)
            val dy = location.y.round(0) - playerLocation.y.round(0)
            val dz = location.z.round(0) - playerLocation.z.round(0)

            val distanceSquared = dx * dx + dy * dy + dz * dz

            distanceSquared <= visibilityDistanceSquared
        }
    }



    fun setup(eventEntitiesManager: EventEntitiesManager) {
        entities = mutableListOf()
        this.eventEntitiesManager = eventEntitiesManager

        spawnDelay = when (level) {
            1 -> 90
            2 -> 80
            3 -> 60
            4 -> 50
            5 -> 30
            else -> 40
        }

        task = object : BukkitRunnable() {
            override fun run() {
                val currentEvent = EventsManager.instance.currentEvent

                // Check whether there is any player within x distance

                if (currentEvent.activePlayers.isEmpty()) {
                    if (entities.isEmpty()) return

                    removeEntities()
                    entities.clear()
                    return
                }

                checkEntities()
            }
        }.runTaskTimer(SkyBlockEvents.instance, 0, 60)

        isSetup = true
    }
    fun stop() {
        task.cancel()
        removeEntities()
    }
    fun reload(eventEntitiesManager: EventEntitiesManager) {
        if(!isSetup) {
            setup(eventEntitiesManager)
            return
        }
        stop()

        setup(eventEntitiesManager)
    }

    private fun checkEntities() {
        checkDeadEntities()
        if(!isAnyPlayerNearby()) return // Don't spawn entities unless there is a player nearby.

        if (entities.size == limit) return

        // Spawn entities only if enough time has passed since the last kill
        if (entities.size < limit) {
            val spawnDelayInMillis = spawnDelay * 1000

            if((System.currentTimeMillis() - lastSpawn) < spawnDelayInMillis) return
            val mob = MythicBukkit.inst().mobManager.spawnMob(entityType, getRandomLocationInCuboid())
            mob.level = level.toDouble()

            val color = when {
                level <= 2 -> "&a"
                level in 3..4 -> "&6"
                else -> "&c"
            }
            val newName = "$color${mob.displayName} &7(poz. $color$level&7)"

            mob.displayName = newName
            mob.showCustomNameplate = true

            entities.add(mob)
            eventEntitiesManager.addEntity(mob.uniqueId, this)

            mob.save()


            lastSpawn = System.currentTimeMillis()
        }
    }
    private fun checkDeadEntities() {
        val deadEntityUUIDs = entities.filter {
            it.isDead
        }
        deadEntityUUIDs.forEach {
            removeEntity(it)
        }
    }

    fun removeEntities() {
        entities.toList().forEach {
            removeEntity(it)
        }
        entities.clear()
    }

    fun removeEntity(activeMob: ActiveMob) {
        Bukkit.getPluginManager()
            .callEvent(MythicMobDeathEvent(activeMob, BukkitAdapter.adapt(activeMob.entity)
                    as LivingEntity, mutableListOf()))

        entities.removeIf { it.uniqueId == activeMob.uniqueId }

    }
    private fun getRandomLocationInCuboid(): Location {
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
}