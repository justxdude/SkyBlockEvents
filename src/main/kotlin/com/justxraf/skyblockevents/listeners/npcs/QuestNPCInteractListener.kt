package com.justxraf.skyblockevents.listeners.npcs

import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.questscore.objectives.objective.NPCInteractionObjective
import com.justxraf.questscore.quests.QuestsManager
import com.justxraf.questscore.users.QuestUserLoadReason
import com.justxraf.questscore.users.QuestUser
import com.justxraf.questscore.users.QuestUsersManager
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager
import com.justxraf.skyblockevents.util.eventsTranslation
import de.oliver.fancynpcs.api.actions.ActionTrigger
import de.oliver.fancynpcs.api.events.NpcInteractEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable

class QuestNPCInteractListener : Listener {
    private val questUserManager = QuestUsersManager.instance
    private val questsManager = QuestsManager.instance

    private val eventsManager = EventsManager.instance
    private val listenersManager = ListenersManager.instance

    @EventHandler
    fun onEntityInteract(event: NpcInteractEvent) {
        val currentEvent = eventsManager.currentEvent
        val player = event.player
        val npcData = event.npc.data

        if(npcData.name != "${currentEvent.uniqueId}_event_npc") return
        if(!listenersManager.doChecks(player.location, currentEvent.spawnLocation)) return

        if(event.interactionType != ActionTrigger.RIGHT_CLICK) return
        var eventQuests = currentEvent.quests

        // attempt to retrieve quests from EventData
        if(eventQuests == null) {
            val eventData = eventsManager.events[currentEvent.uniqueId] ?: return
            val questsCopy = eventData.quests?.toList()
            questsCopy?.forEach {
                currentEvent.addQuest(it)
            }
            eventQuests = currentEvent.quests
        }
        if(eventQuests.isNullOrEmpty()) {
            return
        }

        val questUser = questUserManager.getUser(player.uniqueId, QuestUserLoadReason.DATA_RETRIEVAL) ?: return

        if(questUser.activeQuests.any { eventQuests.contains(it.uniqueId) }) {
            // Check if the goal is related to speaking to the npc
            val objective = questUser
                .findFirstObjective(NPCInteractionObjective::class.java) { it.entityId == "${currentEvent.uniqueId}_event_npc" }
            if(objective == null) {
                val messages = listOf("devil.messages",
                    "devil.messages.two",
                    "devil.messages.three")
                player.sendColoured(messages.random().eventsTranslation(player))
                return
            } else {
                objective.addProgress(1)
            }
        }

        val finishedQuests = questUser.finishedQuests.keys
        val unfinishedQuests = eventQuests.filter { finishedQuests.contains(it).not() }

        val number = unfinishedQuests.minOrNull()
        if(number == null) {
            val messages = listOf("no.more.quests", "no.more.quests.two", "no.more.quests.three")
            player.sendColoured(messages.random().eventsTranslation(player))
            return
        }

        playDialogAndGiveQuest(number, questUser, event.player)
    }
    private fun playDialogAndGiveQuest(number: Int, questUser: QuestUser, player: Player) {
        when (number) {
            10000 -> sendTimedMessages(
                arrayOf("10000.quest.dialogue", "10000.quest.dialogue2", "10000.quest.dialogue3"),
                questUser,
                player,
                number,
                "10000.quest.dialogue4"
            )
            10001 -> sendTimedMessages(
                arrayOf("10001.quest.dialogue", "10001.quest.dialogue2", "10001.quest.dialogue3"),
                questUser,
                player,
                number,
                "10001.quest.dialogue4"
            )
            10002 -> sendTimedMessages(
                arrayOf("10002.quest.dialogue", "10002.quest.dialogue2"),
                questUser,
                player,
                number,
                "10002.quest.dialogue3"
            )
            10003 -> sendTimedMessages(
                arrayOf("10003.quest.dialogue", "10003.quest.dialogue2"),
                questUser,
                player,
                number,
                "10003.quest.dialogue3"
            )
            10004 -> sendTimedMessages(
                arrayOf("10004.quest.dialogue", "10004.quest.dialogue2", "10004.quest.dialogue3"),
                questUser,
                player,
                number,
                "10004.quest.dialogue4"
            )
            10005 -> sendTimedMessages(
                arrayOf("10005.quest.dialogue", "10005.quest.dialogue2"),
                questUser,
                player,
                number,
                "10005.quest.dialogue3"
            )
            10006 -> sendTimedMessages(
                arrayOf("10006.quest.dialogue", "10006.quest.dialogue2", "10006.quest.dialogue3"),
                questUser,
                player,
                number,
                "10006.quest.dialogue4"
            )
            10007 -> sendTimedMessages(
                arrayOf("10007.quest.dialogue", "10007.quest.dialogue2"),
                questUser,
                player,
                number,
                "10007.quest.dialogue3"
            )
            10008 -> sendTimedMessages(
                arrayOf("10008.quest.dialogue", "10008.quest.dialogue2", "10008.quest.dialogue3"),
                questUser,
                player,
                number,
                "10008.quest.dialogue4"
            )
            10009 -> sendTimedMessages(
                arrayOf("10009.quest.dialogue", "10009.quest.dialogue2", "10009.quest.dialogue3"),
                questUser,
                player,
                number,
                "10009.quest.dialogue4"
            )
            10010 -> sendTimedMessages(
                arrayOf("10010.quest.dialogue", "10010.quest.dialogue2", "10010.quest.dialogue3"),
                questUser,
                player,
                number,
                "10010.quest.dialogue4"
            )
            10011 -> sendTimedMessages(
                arrayOf("10011.quest.dialogue", "10011.quest.dialogue2"),
                questUser,
                player,
                number,
                "10011.quest.dialogue3"
            )
            10012 -> sendTimedMessages(
                arrayOf("10012.quest.dialogue", "10012.quest.dialogue2", "10012.quest.dialogue3"),
                questUser,
                player,
                number,
                "10012.quest.dialogue4"
            )
            10013 -> sendTimedMessages(
                arrayOf("10013.quest.dialogue", "10013.quest.dialogue2", "10013.quest.dialogue3"),
                questUser,
                player,
                number,
                "10013.quest.dialogue4"
            )
            10014 -> sendTimedMessages(
                arrayOf("10014.quest.dialogue", "10014.quest.dialogue2"),
                questUser,
                player,
                number,
                "10014.quest.dialogue3"
            )
            10015 -> sendTimedMessages(
                arrayOf("10015.quest.dialogue", "10015.quest.dialogue2", "10015.quest.dialogue3"),
                questUser,
                player,
                number,
                "10015.quest.dialogue4"
            )
            10016 -> sendTimedMessages(
                arrayOf("10016.quest.dialogue", "10016.quest.dialogue2", "10016.quest.dialogue3"),
                questUser,
                player,
                number,
                "10016.quest.dialogue4"
            )
            10017 -> sendTimedMessages(
                arrayOf("10017.quest.dialogue", "10017.quest.dialogue2", "10017.quest.dialogue3"),
                questUser,
                player,
                number,
                "10017.quest.dialogue4"
            )
            10018 -> sendTimedMessages(
                arrayOf("10018.quest.dialogue", "10018.quest.dialogue2", "10018.quest.dialogue3"),
                questUser,
                player,
                number,
                "10018.quest.dialogue4"
            )
            10019 -> sendTimedMessages(
                arrayOf("10019.quest.dialogue", "10019.quest.dialogue2", "10019.quest.dialogue3"),
                questUser,
                player,
                number,
                "10019.quest.dialogue4"
            )
            10020 -> sendTimedMessages(
                arrayOf("10020.quest.dialogue", "10020.quest.dialogue2", "10020.quest.dialogue3"),
                questUser,
                player,
                number,
                "10020.quest.dialogue4"
            )
            10021 -> sendTimedMessages(
                arrayOf("10021.quest.dialogue", "10021.quest.dialogue2", "10021.quest.dialogue3"),
                questUser,
                player,
                number,
                "10021.quest.dialogue4"
            )
            10022 -> sendTimedMessages(
                arrayOf("10022.quest.dialogue", "10022.quest.dialogue2", "10022.quest.dialogue3"),
                questUser,
                player,
                number,
                "10022.quest.dialogue4"
            )
            10023 -> sendTimedMessages(
                arrayOf("10023.quest.dialogue", "10023.quest.dialogue2", "10023.quest.dialogue3", "10023.quest.dialogue4"),
                questUser,
                player,
                number,
                "10023.quest.dialogue5"
            )
        }
    }
    private fun sendTimedMessages(messages: Array<String>, questUser: QuestUser, player: Player, number: Int, description: String) {
        var counter = 0
        val messagesSize = messages.size

        object: BukkitRunnable() {
            override fun run() {
                if(counter > (messagesSize - 1)) {
                    val user = player.asUser() ?: return
                    val quest = questsManager.getQuestBy(number, user) ?: return
                    questUser.giveActiveQuest(quest, false)

                    player.sendColoured(description.eventsTranslation(player))

                    cancel()
                    return
                }
                player.sendColoured(messages[counter].eventsTranslation(player))
                counter++
            }
        }.runTaskTimer(ComponentsManager.instance.plugin, 0, 30L)
    }
}