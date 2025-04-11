package com.justxraf.skyblockevents.listeners.players

import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.eventsTranslation
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class PlayerChatClickListener : Listener {
    private val eventsManager = EventsManager.instance
    @EventHandler
    fun onChatClick(event: PlayerCommandPreprocessEvent) {
        if(!event.message.startsWith("/disable_event_notification")) return
        event.isCancelled = true

        val currentEvent = eventsManager.currentEvent
        val player = event.player

        if(currentEvent.disabledNotifications.contains(player.uniqueId)) {
            player.sendColoured("notifications.already.disabled".eventsTranslation(player))
            return
        }
        currentEvent.disabledNotifications.add(player.uniqueId)
        player.sendColoured("disabled.notifications.for.event".eventsTranslation(player, currentEvent.name.lowercase().eventsTranslation(player)))
    }
}