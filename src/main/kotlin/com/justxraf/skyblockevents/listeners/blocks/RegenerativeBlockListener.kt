package com.justxraf.skyblockevents.listeners.blocks

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.listeners.ListenersManager
import com.justxraf.skyblockevents.util.pushIfClose
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent

class RegenerativeBlockListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action

        if(action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
            val player = event.player

            if (!player.hasPermission("hyperiol.events.admin")) return

            val item = player.itemInHand
            if (item.type != Material.WOODEN_PICKAXE) return

            val itemMeta = item.itemMeta ?: return
            val itemName = itemMeta.itemName

            if (itemName != "regenerative_block_editor") return
            val clickedBlock = event.clickedBlock ?: return

            if (clickedBlock.blockData is Ageable) {
                player.sendColoured("&cTen blok jest rośliną! Użyj narzędzia do modyfikowania regenerujących roślin.")
                return
            }

            if (event.action == Action.RIGHT_CLICK_BLOCK) { // Remove
                val eventFromBlock = eventsManager.events
                    .filter { (_, eventData) ->
                        eventData.regenerativeBlocks != null && eventData.isRegenerativeBlock(clickedBlock.location)
                    }
                    .firstNotNullOfOrNull { it.value }
                if (eventFromBlock == null) {
                    player.sendColoured("&cNie ma tutaj żadnego regenerującego bloku!")
                    event.isCancelled = true
                    return
                }
                if(!eventFromBlock.isRegenerativeBlock(clickedBlock.location)) {
                    player.sendColoured("&cTen blok nie jest na liście regenerujących!")
                    return
                }
                eventFromBlock.regenerativeBlocks?.remove(clickedBlock.location)
                player.sendColoured("&7Usunięto regenerujący blok z wydarzenia o identyfikatorze #${eventFromBlock.uniqueId}.")

                if (eventsManager.currentEvent.uniqueId == eventFromBlock.uniqueId)
                    eventsManager.currentEvent.regenerativeBlocks?.remove(clickedBlock.location)

                eventsManager.saveEvent(eventFromBlock)

            } else {
                val selectedEvent = eventsManager.events.filter { (_, value) -> value.spawnLocation.world == event.player.world }
                    .firstNotNullOfOrNull { it.value }
                if (selectedEvent == null) {
                    player.sendColoured("&cNie możesz ustawić regenerujących bloków w tym świecie, ponieważ nie ma w nim żadnych wydarzeń!")
                    event.isCancelled = true
                    return
                }
                if(selectedEvent.isRegenerativeBlock(clickedBlock.location)) {
                    player.sendColoured("&cTen blok jest już dodany do listy!")
                    return
                }

                player.sendColoured("&aDodano regenerujący blok do wydarzenia o identyfikatorze #${selectedEvent.uniqueId}.")
                selectedEvent.addRegenerativeBlock(clickedBlock.location, clickedBlock.type)

                if(eventsManager.currentEvent.uniqueId == selectedEvent.uniqueId)
                    eventsManager.currentEvent.addRegenerativeBlock(clickedBlock.location, clickedBlock.type)

                eventsManager.saveEvent(selectedEvent)
            }
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent

        val block = event.block
        val location = block.location

        if (!listenersManager.doChecks(location, currentEvent.spawnLocation)) return

        if (!currentEvent.isRegenerativeBlock(location)
            && !event.player.hasPermission("hyperiol.events.admin")) {
            event.isCancelled = true
            return
        }

        currentEvent.breakRegenerativeBlockAt(location)

    }
    @EventHandler
    fun onRegenerativeBlockPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player

        if (!player.hasPermission("hyperiol.events.admin")) return
        val item = player.itemInHand
        if (item.type == Material.AIR) return

        val itemMeta = item.itemMeta ?: return
        val itemName = itemMeta.itemName

        if (itemName != "regenerative_block") return
        val selectedEvent = eventsManager.events.filter { (_, value) -> value.spawnLocation.world == event.player.world }
            .firstNotNullOfOrNull { it.value }
        if (selectedEvent == null) {
            player.sendColoured("&cNie możesz ustawić regenerującego bloku w tym świecie, ponieważ nie ma w nim żadnych wydarzeń!")
            event.isCancelled = true
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        val location = clickedBlock.location

        event.isCancelled = true
        location.block.type = item.type

        if (selectedEvent.regenerativeBlocks == null) selectedEvent.regenerativeBlocks = mutableMapOf()
        selectedEvent.regenerativeBlocks!![location] = item.type

    }
}