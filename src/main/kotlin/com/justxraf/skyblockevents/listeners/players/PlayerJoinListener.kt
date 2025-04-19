package com.justxraf.skyblockevents.listeners.players

import com.justxdude.islandcore.islands.islandmanager.IslandManager.Companion.island
import com.justxraf.networkapi.util.sendColoured
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserSettingsFlag
import com.justxraf.questscore.users.QuestUserLoadReason
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    private val eventsManager = EventsManager.instance

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val user = player.asUser() ?: return
        val currentEvent = eventsManager.currentEvent

        if(user.level < currentEvent.requiredLevel) return

        if(currentEvent.eventUserHandler.users.contains(player.uniqueId)) {
            if(user.getFlagBoolean(UserSettingsFlag.ALLOW_EVENT_NOTIFICATIONS)) {
                currentEvent.joinMessage().forEach {
                    player.sendColoured(it)
                }
            }
        } else {
            if(user.getFlagBoolean(UserSettingsFlag.ALLOW_EVENT_NOTIFICATIONS)) {
                currentEvent.startMessage().forEach {
                    player.sendColoured(it)
                }
            }
            val questUser = UsersManager.instance.getUser(player.uniqueId, QuestUserLoadReason.DATA_RETRIEVAL) ?: return
            currentEvent.eventUserHandler.restartQuestsFor(questUser)
        }
        val eventUser = currentEvent.eventUserHandler.getUser(event.player.uniqueId)
        eventUser.lastCache = System.currentTimeMillis()
    }
    // World check
    @EventHandler
    fun onPlayerJoinWorld(event: PlayerJoinEvent) {
        val player = event.player

        val currentEvent = eventsManager.currentEvent

        val events = eventsManager.events.values
        // Check if the player is in the same world as any of the events
        val worldEvent = events.firstOrNull { it.spawnLocation.world == player.world } ?: return

        if(worldEvent.uniqueId == currentEvent.uniqueId) {
            if(currentEvent.eventUserHandler.users.contains(player.uniqueId)) return
        }
        if(player.hasPermission("hyperiol.events.admin")) return

        val skyBlockUser = player.asUser() ?: return
        val island = skyBlockUser.island

        if(island != null) island.teleportHome(skyBlockUser, false)
                else SkyblockAPI.instance.spawn.teleport(player)
    }
}