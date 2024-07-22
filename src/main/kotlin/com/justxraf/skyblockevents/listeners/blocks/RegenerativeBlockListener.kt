package com.justxraf.skyblockevents.listeners.nether.blocks

import com.github.supergluelib.foundation.giveOrDropItem
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.listeners.ListenersManager
import com.justxraf.skyblockevents.util.pushPlayerIfClose
import kotlinx.coroutines.selects.select
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent

class RegenerativeBlockListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance
    @EventHandler
    fun onRegenerativeBlockRemove(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK || event.action != Action.LEFT_CLICK_BLOCK) return
        val player = event.player

        if (!player.hasPermission("hyperiol.events.admin")) return

        val item = player.itemInHand
        if (item.type != Material.WOODEN_PICKAXE) return

        val itemMeta = item.itemMeta ?: return
        val itemName = itemMeta.itemName

        if (itemName != "regenerative_block_remover") return
        val clickedBlock = event.clickedBlock ?: return

        val eventFromBlock = eventsManager.events
            .filter { (_, eventData) ->
                eventData.regenerativeBlocks != null && eventData.regenerativeBlocks?.get(clickedBlock.location) != null
            }
            .firstNotNullOfOrNull { it.value }
        if (eventFromBlock == null) {
            player.sendColoured("&cNie ma tutaj żadnego regenerującego bloku!")
            event.isCancelled = true
            return
        }

        eventFromBlock.regenerativeBlocks?.remove(clickedBlock.location)
        player.sendColoured("&7Usunięto regenerujący blok z wydarzenia o ID ${eventFromBlock.uniqueId}.")

        if (eventsManager.currentEvent.uniqueId == eventFromBlock.uniqueId)
            eventsManager.currentEvent.regenerativeBlocks?.remove(clickedBlock.location)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val currentEvent = eventsManager.currentEvent
        if (currentEvent !is NetherEvent) return

        val block = event.block
        val location = block.location

        if (!listenersManager.doChecks(location, currentEvent.spawnLocation)) return

        if (!currentEvent.isRegenerativeBlock(location)
            && !event.player.hasPermission("hyperiol.events.admin")
        ) {
            event.isCancelled = true
            return
        }

        currentEvent.breakRegenerativeBlockAt(location)

    }
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val currentEvent = eventsManager.currentEvent
        val questNpcLocation = currentEvent.questNPCLocation ?: return

        if(questNpcLocation.world != event.player.world) return

        pushPlayerIfClose(questNpcLocation, event.player, 1.3, .15)
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
        val selectedEvent = eventsManager.events.filter { (_, value) -> value.world == event.player.world.name }
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