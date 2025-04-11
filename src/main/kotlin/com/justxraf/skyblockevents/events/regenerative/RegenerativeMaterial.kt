package com.justxraf.skyblockevents.events.regenerative

import com.justxraf.skyblockevents.events.Event
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import kotlin.math.abs

class RegenerativeMaterial(
    val material: Material,
    val isHarvestable: Boolean
) {
    @Transient
    private var brokenBlocks: MutableMap<Location, Long>? = mutableMapOf()

    fun setup() {
        brokenBlocks = mutableMapOf()
    }
    fun reload() {
        if(brokenBlocks.isNullOrEmpty()) brokenBlocks = mutableMapOf()
        brokenBlocks?.forEach {
            regenerateBlock(it.key)
        }
        brokenBlocks?.clear()
    }
    fun end() {
        if(brokenBlocks.isNullOrEmpty()) brokenBlocks = mutableMapOf()

        brokenBlocks?.forEach {
            regenerateBlock(it.key)
        }
        brokenBlocks?.clear()
    }

    fun checkBrokenBlocks(event: Event) {
        val blocksToRegenerate = mutableListOf<Location>()

        brokenBlocks?.forEach { (location, breakTime) ->
            val distance = getDistanceFromSpawnPoint(event, location)

            val delay = when {
                distance < 100 -> 120
                distance < 140 && distance > 100 -> 100
                distance < 180 && distance > 140 -> 60
                distance < 210 && distance > 180 -> 30
                else -> 30
            }
            if (System.currentTimeMillis() - breakTime > (delay * 1000)) {
                blocksToRegenerate.add(location)
            }
        }

        if(blocksToRegenerate.isNotEmpty()) {
            blocksToRegenerate.forEach { location ->
                regenerateBlock(location)
                brokenBlocks?.remove(location)
            }
        }
    }
    private fun regenerateBlock(location: Location) {
        val block = location.block
        block.type = material

        if(isHarvestable) {
            val ageable = block.state as? Ageable ?: return
            ageable.age = 0
        }
    }
    private fun getDistanceFromSpawnPoint(event: Event, blockLocation: Location): Int {
        val loc1 = event.spawnLocation
        return abs(loc1.blockX - blockLocation.blockX) +
                abs(loc1.blockY - blockLocation.blockY) +
                abs(loc1.blockZ - blockLocation.blockZ)
    }
    fun canBreak(location: Location): Boolean =
        if(isHarvestable) {
            val ageable = location.block.blockData as Ageable

            ageable.age == ageable.maximumAge
        } else true
    fun breakMaterial(location: Location) {
        if(brokenBlocks.isNullOrEmpty()) brokenBlocks = mutableMapOf()
        brokenBlocks?.put(location, System.currentTimeMillis())
    }
}