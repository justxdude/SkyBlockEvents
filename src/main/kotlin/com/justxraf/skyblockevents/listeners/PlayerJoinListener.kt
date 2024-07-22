package com.justxraf.skyblockevents.listeners

import com.justxdude.islandcore.islands.islandmanager.IslandManager.Companion.island
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.formatDuration
import com.justxraf.skyblockevents.util.toTimeAgo
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoinListener : Listener {
    private val eventsManager = EventsManager.instance
    private val questsUserManager = UsersManager.instance



    // TODO Fix deletion of quests on player join :)


    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val user = player.asUser() ?: return
        val currentEvent = eventsManager.currentEvent

        if(user.level < currentEvent.requiredLevel) return

        if(currentEvent.playersWhoJoined.contains(player.uniqueId)) {
            player.sendColoured("&9&m-".repeat(30))
            player.sendColoured("&7Wydarzenie ${currentEvent.name} kończy się za ${(currentEvent.endsAt - System.currentTimeMillis()).formatDuration()}!")
            player.sendColoured("&bWydobądź wszystkie surowce i wykonaj zadania, zanim minie czas!")
            player.sendColoured("&9&m-".repeat(30))
        } else {
            currentEvent.startMessage().forEach {
                player.sendColoured(it)
            }
        }
    }
    // World check
    @EventHandler
    fun onPlayerJoinWorld(event: PlayerJoinEvent) {
        val player = event.player
        val currentEvent = eventsManager.currentEvent

        val location = player.location
        if(currentEvent.spawnLocation == player.location) return

        if(eventsManager.events.firstNotNullOfOrNull { it.value.spawnLocation.world == location.world } == null) return
        if(player.hasPermission("hyperiol.events.admin")) return

        val skyBlockUser = player.asUser() ?: return
        val island = skyBlockUser.island

        if(island != null) island.teleportHome(skyBlockUser, false, true)
                else SkyblockAPI.instance.spawn.teleport(player)
    }
    @EventHandler
    fun onPlayerJoinQuests(event: PlayerQuitEvent) {
        val player = event.player
        val currentEvent = eventsManager.currentEvent

        val questUser = questsUserManager.getUser(player.uniqueId) ?: return
        val quests = currentEvent.quests ?: return

        val keysToRemove =
            questUser.finishedQuests.filter { quests.contains(it.key) && it.value.time < currentEvent.startedAt }
                .map { it.key }

        keysToRemove.forEach { questUser.finishedQuests.remove(it) }
    }
}