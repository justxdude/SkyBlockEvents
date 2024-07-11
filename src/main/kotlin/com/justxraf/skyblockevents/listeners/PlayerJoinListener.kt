package com.justxraf.skyblockevents.listeners

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.toTimeAgo
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
        val currentEvent = eventsManager.currentEvent

        if(!currentEvent.playersWhoJoined.contains(player.uniqueId)) {
            if(currentEvent.portalLocation != null) {
                player.sendColoured("&aNowe wydarzenie zatytuowana ${currentEvent.name} wystartowało ${currentEvent.startedAt.toTimeAgo()} temu! " +
                        "Na spawnie pojawił się portal, przez który możesz dołączyć do tego wydarzenia!")
            } else {
                player.sendColoured("&aWydarzenie ${currentEvent.name} wystartowało ${currentEvent.startedAt.toTimeAgo()} temu!")
            }
        }
    }
    // World check
    @EventHandler
    fun onPlayerJoinWorld(event: PlayerJoinEvent) {
        val player = event.player
        val currentEvent = eventsManager.currentEvent

        // Loop through events list from EventsManager and check whether any of the worlds is
        // the same as the players' world. If the world is the same as the current event - return.
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