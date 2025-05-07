package com.justxraf.skyblockevents.listeners.players

import com.justxdude.islandcore.api.player.CustomPlayerDeathEvent
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager
import com.justxraf.skyblockevents.util.eventsTranslation
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class PlayerDeathListener : Listener {
    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDeath(event: CustomPlayerDeathEvent) {
        val currentEvent = eventsManager.currentEvent
        val player = event.player

        if(!listenersManager.doChecks(player.location, currentEvent.spawnLocation)) return

        event.isCancelled = true

        player.sendColoured("you.have.died".eventsTranslation(player))
        player.playSound(player.location, Sound.ENTITY_VILLAGER_DEATH, 3F, 1F)

        val user = currentEvent.eventUserHandler.getUser(player.uniqueId)
        user?.deaths += 1

        player.teleport(currentEvent.spawnLocation)
    }
}