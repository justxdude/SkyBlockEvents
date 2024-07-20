package com.justxraf.skyblockevents.listeners.nether.blocks

import com.justxdude.islandcore.listenersv2.world.offline.OfflineGrowthListener
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.player.PlayerInteractEvent

class RegenerativePlantListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK || event.action != Action.LEFT_CLICK_BLOCK) return
        val player = event.player

        if (!player.hasPermission("hyperiol.events.admin")) return

        val item = player.itemInHand
        if (item.type != Material.WOODEN_HOE) return

        val itemMeta = item.itemMeta ?: return
        val itemName = itemMeta.itemName

        if (itemName != "regenerative_plant_editor") return
        val clickedBlock = event.clickedBlock ?: return

        if(event.action == Action.RIGHT_CLICK_BLOCK) { // Remove

            val eventFromBlock = eventsManager.events
                .filter { (_, eventData) ->
                    eventData.regenerativePlants != null && eventData.regenerativePlants?.get(clickedBlock.location) != null
                }
                .firstNotNullOfOrNull { it.value }
            if (eventFromBlock == null) {
                player.sendColoured("&cNie ma tutaj żadnej regenerującej rośliny!")
                event.isCancelled = true
                return
            }
            eventFromBlock.regenerativePlants?.remove(clickedBlock.location)
            player.sendColoured("&7Usunięto regenerującą roślinę z wydarzenia o ID ${eventFromBlock.uniqueId}.")

            if (eventsManager.currentEvent.uniqueId == eventFromBlock.uniqueId)
                eventsManager.currentEvent.regenerativePlants?.remove(clickedBlock.location)

        } else { // Add
            val selectedEvent = eventsManager.events.filter { (_, value) -> value.world == event.player.world.name }
                .firstNotNullOfOrNull { it.value }
            if (selectedEvent == null) {
                player.sendColoured("&cNie możesz ustawić regenerującej rośliny w tym świecie, ponieważ nie ma w nim żadnych wydarzeń!")
                event.isCancelled = true
                return
            }
            selectedEvent.addRegenerativePlant(player.location, clickedBlock.type)
        }
    }
    @EventHandler
    fun onPlantBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent

        val block = event.block
        if(block.location.world != currentEvent.spawnLocation.world) return

        val player = event.player
        val location = block.location

        if (!currentEvent.isRegenerativePlant(location)) return
        if(!currentEvent.canHarvestRegenerativePlant(location)) {
            player.sendColoured("&cDaj temu urosnąć")
            event.isCancelled = true
            return
        }
        currentEvent.harvestRegenerativePlant(location)
    }
    @EventHandler
    fun onPlantGrowth(event: BlockGrowEvent) {
        val currentEvent = eventsManager.currentEvent
        val block = event.block

        if(block.location.world != currentEvent.spawnLocation.world) return
        if(currentEvent.activePlayers.isNotEmpty()) return

        event.isCancelled
    }
}