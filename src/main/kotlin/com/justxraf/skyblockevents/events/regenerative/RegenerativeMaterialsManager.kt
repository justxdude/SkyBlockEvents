package com.justxraf.skyblockevents.events.regenerative

import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.Event
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class RegenerativeBlocksManager(
    var regenerativeBlocks: MutableList<Material> = mutableListOf(),
    private var task: BukkitTask? = null,
) {
    @Transient
    private var brokenBlocks: MutableMap<Location, RegenerativeBlock> = mutableMapOf()

    fun setup(event: Event) {
        println("Starting regenerative blocks manager with ${regenerativeBlocks.size} regenerative blocks!")

        assignLocations()

        reloadRegenerativeBlocks(event)
        startTask(event)

        println("RegenerativeBlocksManager is now on!")
    }

    private fun assignLocations() {
        regenerativeBlockLocations = mutableMapOf()
        regenerativeBlocks.associateByTo(regenerativeBlockLocations) { it.location }
    }

    fun reload(event: Event) {
        stop()
        setup(event)
    }

    fun startTask(event: Event) {
        task = object : BukkitRunnable() {
            override fun run() {
                checkRegenerativeBlocks(event)
            }
        }.runTaskTimer(SkyBlockEvents.instance, 0, 20 * 1) // Check every 5 seconds.
    }
    fun stop() {
        task?.cancel()
    }
    fun addRegenerativeBlock(
        event: Event,
        location: Location,
        material: Material,
        isHarvestable: Boolean,
    ) {
        if(isRegenerativeBlock(location)) return

        val newBlock = RegenerativeBlock(material, location, isHarvestable)
        newBlock.setup(event)


        regenerativeBlocks.add(RegenerativeBlock(material, location, isHarvestable))
    }

    fun removeBlock(location: Location) {
        val block = regenerativeBlockLocations[location] ?: return
        block.remove()

        regenerativeBlocks.remove(block)
        regenerativeBlockLocations.remove(location)
    }
    fun isRegenerativeBlock(location: Location): Boolean =
        regenerativeBlockLocations[location] != null

    fun breakRegenerativeBlockAt(location: Location) {
        val block = regenerativeBlockLocations[location] ?: return
        block.remove()
    }
    fun canBreakRegenerativeBlockAt(location: Location): Boolean {
        val block = regenerativeBlockLocations[location] ?: return false
        return block.canBreak()
    }
    private fun reloadRegenerativeBlocks(event: Event) {
        regenerativeBlocks.forEach { it.reload(event) }
    }
    private fun checkRegenerativeBlocks(event: Event) {
        regenerativeBlockLocations.values.forEach { it.check(event) }
    }
}