package com.justxraf.skyblockevents.listeners.players

import com.justxdude.islandcore.islands.islandmanager.IslandManager.Companion.island
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.networkapi.util.Utils.toDate
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.questscore.quests.QuestsManager
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.formatDuration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoinListener : Listener {
    private val eventsManager = EventsManager.instance
    private val questsUserManager = UsersManager.instance

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val user = player.asUser() ?: return
        val currentEvent = eventsManager.currentEvent

        if(user.level < currentEvent.requiredLevel) return

        if(currentEvent.playersWhoJoined.contains(player.uniqueId)) {
            player.sendColoured("&9&m-".repeat(30))
            player.sendColoured("&7Wydarzenie ${currentEvent.name} kończy się o ${currentEvent.endsAt.toDate()}!")
            player.sendColoured("&bWydobądź wszystkie surowce i wykonaj zadania, zanim minie czas!")
            player.sendColoured("&9&m-".repeat(30))

            println(currentEvent.endsAt - System.currentTimeMillis())
        } else {
            currentEvent.startMessage().forEach {
                player.sendColoured(it)
            }

            val questUser = UsersManager.instance.getUser(player.uniqueId) ?: return
            currentEvent.restartQuestsFor(questUser)
        }
    }
    // World check
    @EventHandler
    fun onPlayerJoinWorld(event: PlayerJoinEvent) {
        val player = event.player
        val user = player.asUser() ?: return

        val currentEvent = eventsManager.currentEvent

        val events = eventsManager.events.values
        // Check if the player is in the same world as any of the events
        val worldEvent = events.firstOrNull { it.spawnLocation.world == player.world } ?: return

        if(worldEvent.uniqueId == currentEvent.uniqueId) {
            if(currentEvent.playersWhoJoined.contains(player.uniqueId)) return
        }
        if(player.hasPermission("hyperiol.events.admin")) return

        val skyBlockUser = player.asUser() ?: return
        val island = skyBlockUser.island

        if(island != null) island.teleportHome(skyBlockUser, false)
                else SkyblockAPI.instance.spawn.teleport(player)
    }
}