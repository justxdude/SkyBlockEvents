package com.justxraf.skyblockevents.listeners.points

import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class BlockBreakListener : Listener {
    val eventsManager = EventsManager.instance

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent
        val world = event.block.world
        if(currentEvent.spawnLocation.world != world) return

        val player = event.player
        currentEvent.pointsHandler.addPoints(player.uniqueId, getReward(event.block.type))
    }
    private fun getReward(material: Material): Int {
        return when(material) {
            Material.NETHER_WART -> 4
            Material.NETHERRACK -> 1
            Material.SOUL_SAND -> 1
            Material.SOUL_SOIL -> 1
            Material.BASALT -> 2
            Material.SMOOTH_BASALT -> 2
            Material.BLACKSTONE -> 2
            Material.GILDED_BLACKSTONE -> 4
            Material.MAGMA_BLOCK -> 2
            Material.NETHER_GOLD_ORE -> 3
            Material.NETHER_QUARTZ_ORE -> 2
            Material.GLOWSTONE -> 2
            Material.SHROOMLIGHT -> 2
            Material.CRIMSON_NYLIUM -> 2
            Material.WARPED_NYLIUM -> 2
            Material.CRIMSON_STEM -> 2
            Material.WARPED_STEM -> 2
            Material.STRIPPED_CRIMSON_STEM -> 2
            Material.STRIPPED_WARPED_STEM -> 2
            Material.CRIMSON_ROOTS -> 1
            Material.WARPED_ROOTS -> 1
            Material.NETHER_SPROUTS -> 1
            Material.WEEPING_VINES -> 1
            Material.TWISTING_VINES -> 1
            else -> 1
        }
    }
}