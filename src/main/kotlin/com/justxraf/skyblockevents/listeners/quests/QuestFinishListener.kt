package com.justxraf.skyblockevents.listeners.quests

import com.justxraf.questscore.api.UserQuestFinishEvent
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class QuestFinishListener : Listener {
    val eventsManager = EventsManager.instance

    @EventHandler
    fun onQuestFinish(event: UserQuestFinishEvent) {
        val currentEvent = eventsManager.currentEvent

        val quests = currentEvent.quests
        if(quests == null) return

        if(!quests.contains(event.quest.uniqueId)) return
        val eventUserHandler = currentEvent.eventUserHandler

        val user = eventUserHandler.getUser(event.user.uniqueId) ?: return
        user.questsFinished += event.quest.uniqueId
    }
}