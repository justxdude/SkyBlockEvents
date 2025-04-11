package com.justxraf.skyblockevents.events.player

import com.justxdude.skyblockapi.SkyblockAPI
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.util.eventsTranslation
import org.bukkit.World
import org.bukkit.entity.Player

class EventPlayer(
    val player: Player,
    var lastCache: Long,

    ) {
    fun shouldRemove(world: World): Boolean {
        val delay = 300 * 1000 // 5 minutes
        return lastCache + delay < System.currentTimeMillis() || world != player.world
    }
    fun kick(world: World) {
        if(world != player.world) return

        SkyblockAPI.instance.spawn.teleport(player, false)
        player.sendColoured("no.activity".eventsTranslation(player))
    }
}