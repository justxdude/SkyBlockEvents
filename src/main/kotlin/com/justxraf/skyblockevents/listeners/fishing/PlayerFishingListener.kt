package com.justxraf.skyblockevents.listeners.fishing

import com.github.supergluelib.foundation.giveOrDropItem
import com.github.supergluelib.foundation.util.ItemBuilder
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
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
        ItemDrop(ItemDropType.ENTITY, 1, "SkeletalKnight"),
        ItemDrop(ItemDropType.MATERIAL, 1, "", ItemBuilder(Material.DIAMOND_PICKAXE, "Magiczny kilof").build())
    )
    @EventHandler
    fun onPlayerFishing(event: PlayerFishEvent) {
        if(event.state != PlayerFishEvent.State.CAUGHT_FISH) return

        val player = event.player
        val currentEvent = eventsManager.currentEvent

//        if(currentEvent.eventType != EventType.FISHING) return
//        if(!listenersManager.doChecks(player.location, currentEvent.spawnLocation)) return

        val item = drop.random()

        when(item.type) {
            ItemDropType.ENTITY -> {
                val mob = MythicBukkit.inst().mobManager.getMythicMob(item.entityName).orElse(null)
                val spawned = mob.spawn(BukkitAdapter.adapt(player.location), Random.nextDouble(1.0, 3.0))
                spawned.showCustomNameplate = true

                spawned.setTarget(BukkitAdapter.adapt(player))

                player.sendColoured("&cUważaj! Złowiłeś(aś) ${spawned.displayName}!")
            }
            ItemDropType.MATERIAL -> {
                player.giveOrDropItem(item.item)

                player.sendColoured("&7Złowiłeś(aś) ${item.item.itemMeta?.displayName}&7!")
            }
            ItemDropType.MONEY -> {
                val user = player.asUser() ?: return
                user.addMoney(item.amount.toDouble())

                player.sendColoured("&7Złowiłeś(aś) &6$${item.amount.toDouble().toBigDecimal()}&7!")
            }
        }
    }
}
private class ItemDrop(
    val type: ItemDropType,
    val amount: Int = 1,
    val entityName: String = "null",
    val item: ItemStack = ItemBuilder(Material.STONE).build()
)
private enum class ItemDropType {
    MONEY, ENTITY, MATERIAL
}