package com.justxraf.skyblockevents.listeners.plants

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.shouldSendMessage
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

class RegenerativePlantListener : Listener {
    private val eventsManager = EventsManager.instance
    private val timer: MutableMap<UUID, Long> = mutableMapOf()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action

        if(action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
            val player = event.player

            if (!player.hasPermission("hyperiol.events.admin")) return

            val item = player.itemInHand
            if (item.type != Material.WOODEN_HOE) return

            val itemMeta = item.itemMeta ?: return
            val itemName = itemMeta.itemName

            if (itemName != "regenerative_plant_editor") return
            val clickedBlock = event.clickedBlock ?: return

            if (clickedBlock.blockData !is Ageable) {
                player.sendColoured("&cTen blok nie jest rośliną! Użyj narzędzia do modyfikowania bloków.")
                return
            }

            if (event.action == Action.RIGHT_CLICK_BLOCK) { // Remove
                val eventFromBlock = eventsManager.events
                    .filter { (_, eventData) ->
                        eventData.regenerativePlants != null && eventData.isRegenerativePlant(clickedBlock.location)
                    }
                    .firstNotNullOfOrNull { it.value }
                if (eventFromBlock == null) {
                    player.sendColoured("&cNie ma tutaj żadnej regenerującej rośliny!")
                    event.isCancelled = true
                    return
                }
                if(!eventFromBlock.isRegenerativePlant(clickedBlock.location)) {
                    player.sendColoured("&cTa roślina nie jest na liście regenerujących!")
                    return
                }
                eventFromBlock.regenerativePlants?.remove(clickedBlock.location)
                player.sendColoured("&7Usunięto regenerującą roślinę z wydarzenia o ID ${eventFromBlock.uniqueId}.")

                if (eventsManager.currentEvent.uniqueId == eventFromBlock.uniqueId)
                    eventsManager.currentEvent.regenerativePlants?.remove(clickedBlock.location)

                eventsManager.saveEvent(eventFromBlock)

            } else { // Add
                val selectedEvent = eventsManager.events.filter { (_, value) -> value.spawnLocation.world == event.player.world }
                    .firstNotNullOfOrNull { it.value }
                if (selectedEvent == null) {
                    player.sendColoured("&cNie możesz ustawić regenerującej rośliny w tym świecie, ponieważ nie ma w nim żadnych wydarzeń!")
                    event.isCancelled = true
                    return
                }
                if(selectedEvent.isRegenerativePlant(clickedBlock.location)) {
                    player.sendColoured("&cTa roślina jest już dodana do listy!")
                    return
                }

                player.sendColoured("&aDodano regenerującą roślinę do wydarzenia o identyfikatorze ${selectedEvent.uniqueId}.")
                selectedEvent.addRegenerativePlant(clickedBlock.location, clickedBlock.type)

                if(eventsManager.currentEvent.uniqueId == selectedEvent.uniqueId)
                    eventsManager.currentEvent.addRegenerativePlant(clickedBlock.location, clickedBlock.type)

                eventsManager.saveEvent(selectedEvent)
            }
            event.isCancelled = true
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
            if(timer.shouldSendMessage(player.uniqueId)) player.sendColoured("&cDaj temu urosnąć")
            event.isCancelled = true
            return
        }
        currentEvent.harvestRegenerativePlant(location)
    }
}