package com.justxraf.skyblockevents.listeners.fishing

import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import kotlin.random.Random

class PlayerFishingListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance

    /*

    Add support for MythicMobs and spawn monsters for players
    Add custom drop after killing a MythicMobs mob
    Add messages once mythic mob is spawned (spawn only when player fishes sth)
    Set target to only be the player who caught it


     */
    private val drop: MutableList<ItemDrop> = mutableListOf(
        ItemDrop(ItemDropType.MONEY, 1000),
        ItemDrop(ItemDropType.ENTITY, 1, "Entity")
    )
    @EventHandler
    fun onPlayerFishing(event: PlayerFishEvent) {
        val player = event.player
        val currentEvent = eventsManager.currentEvent

        if(currentEvent.eventType != EventType.FISHING) return
        if(!listenersManager.doChecks(player.location, currentEvent.spawnLocation)) return

        event.isCancelled = true
        //todo make random
    }
}
private class ItemDrop(
    val itemDropType: ItemDropType,
    val amount: Int = 1,
    val entityName: String = "null"
)
private enum class ItemDropType {
    MONEY, ENTITY, MATERIAL
}