package com.justxraf.skyblockevents.listeners.nether

import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxraf.questscore.objectives.objective.NPCInteractionObjective
import com.justxraf.questscore.quests.QuestsManager
import com.justxraf.questscore.users.QuestUser
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.EventsManager
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class QuestNPCInteractListener : Listener {
    private val questUserManager = UsersManager.instance
    private val questsManager = QuestsManager.instance

    private val eventsManager = EventsManager.instance
    private val timeChecker: MutableMap<UUID, Long> = mutableMapOf()

    private val alreadySpeakingToNPC: MutableList<UUID> = mutableListOf()
    @EventHandler
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked

        if(!CitizensAPI.getNPCRegistry().isNPC(entity)) return
        val npc = CitizensAPI.getNPCRegistry().getNPC(entity)

        val currentEvent = eventsManager.currentEvent
        if(currentEvent.questNPCUniqueId != npc.id) return

        val player = event.player

        if(!shouldProcessNPCInteraction(player.uniqueId)) return
        if(alreadySpeakingToNPC.contains(player.uniqueId)) return

        val eventQuests = currentEvent.quests ?: return
        val questUser = questUserManager.getUser(event.player.uniqueId) ?: return

        if(questUser.activeQuests.any { eventQuests.contains(it.uniqueId) }) {
            // Check if the objective is related to speaking to the npc
            // Check if the objective is related to speaking to this npc
            val objective = questUser
                .findFirstObjective(NPCInteractionObjective::class.java) { it.entityId == npc.id }
            if(objective == null) {
                val messages = listOf("&cCo Ty robisz? Ukończ najpierw zadania!",
                    "&cZagadaj do mnie jak ukończysz zadania, głupcze",
                    "&cIgrasz z ogniem xD")
                player.sendColoured(messages.random())
                return
            } else {
                objective.addProgress(1)
            }
        }

        val finishedQuests = questUser.finishedQuests.keys
        val unfinishedQuests = eventQuests.filter { it !in finishedQuests }

        val number = unfinishedQuests.minOrNull() ?: eventQuests.min()
        playDialogAndGiveQuest(number, questUser, event.player)
    }
    private fun playDialogAndGiveQuest(number: Int, questUser: QuestUser, player: Player) {
        when(number) {
            10000 -> sendTimedMessages(
                arrayOf("&cWitaj w piekle...", "&cMam dla Ciebie kilka zadań...", "&cWykonaj je i dostaniesz piekielne nagrody..."),
                questUser,
                player,
                number,
                "&7Otrzymałeś/aś pierwsze zadanie od diabła! Wykonaj je i powróć do niego po więcej! (użyj \"/zadania normalne\" aby sprawdzić zadanie)"
            )
            10001 -> sendTimedMessages(
                arrayOf("&cCzy odważysz się wkroczyć w samo serce piekielnych czeluści, śmiertelniku? Tylko najodważniejsi mogą przetrwać próbę, która tam na ciebie czeka.",
                    "Mroczne siły czekają na twoje potknięcie. Pokaż, że nie boisz się ognia ani cienia. Udowodnij swoją wartość i wróć zwycięsko.",
                    "W piekle każdy krok może być twoim ostatnim. Tylko prawdziwi wojownicy mają w sobie odwagę, by stawić czoła temu, co tam na nich czeka. " +
                            "Pytanie, czy jesteś jednym z nich?"),
                questUser,
                player,
                number,
                "&7Otrzymałeś/aś drugie zadanie od diabła! Wykonaj je i powróć do niego po więcej!"
            )
        }
    }
    private fun sendTimedMessages(messages: Array<String>, questUser: QuestUser, player: Player, number: Int, description: String) {
        var counter = 0
        val messagesSize = messages.size
        alreadySpeakingToNPC.add(player.uniqueId)

        object: BukkitRunnable() {
            override fun run() {
                if(counter > (messagesSize - 1)) {
                    questUser.giveActiveQuest(questsManager.getQuestBy(number) ?: return, false)

                    player.sendColoured(description)
                    alreadySpeakingToNPC.remove(player.uniqueId)

                    cancel()
                    return
                }
                player.sendColoured(messages[counter])
                counter++
            }
        }.runTaskTimer(ComponentsManager.instance.plugin, 0, 30L)
    }
    private fun shouldProcessNPCInteraction(uniqueId: UUID): Boolean {
        if(timeChecker[uniqueId] == null) {
            timeChecker[uniqueId] = System.currentTimeMillis()
            return true
        }
        if(System.currentTimeMillis() - timeChecker[uniqueId]!! > 3000) {
            timeChecker[uniqueId] = System.currentTimeMillis()
            return true
        }
        return false
    }
}