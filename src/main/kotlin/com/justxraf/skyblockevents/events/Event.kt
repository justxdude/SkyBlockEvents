package com.justxraf.skyblockevents.events

import com.justxdude.islandcore.islands.Island
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.networkapi.util.sendColouredActionBar
import com.justxraf.questscore.quests.quest.FinishedQuest
import com.justxraf.questscore.users.QuestUserLoadReason
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.events.data.FinishedEvent
import com.justxraf.skyblockevents.events.data.user.EventUserData
import com.justxraf.skyblockevents.events.entities.EventEntitiesHandler
import com.justxraf.skyblockevents.events.portals.EventPortal
import com.justxraf.skyblockevents.events.portals.EventPortalType
import com.justxraf.skyblockevents.events.portals.PortalRemovalReason
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterialsHandler
import com.justxraf.skyblockevents.translations.SkyBlockEventsResourcesManager
import com.justxraf.skyblockevents.users.EventStatistic
import com.justxraf.skyblockevents.users.EventUserHandler
import com.justxraf.skyblockevents.util.eventsTranslation
import com.justxraf.skyblockevents.util.getEndOfDayMillis
import com.justxraf.skyblockevents.util.getRankColor
import com.justxraf.skyblockevents.util.isInCuboid
import com.justxraf.skyblockevents.util.localeEventsTranslation
import com.justxraf.skyblockevents.util.removeViewer
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.HologramManager
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.data.property.Visibility
import de.oliver.fancyholograms.api.hologram.Hologram
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.NpcData
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.map
import kotlin.jvm.optionals.getOrNull

open class Event(
    open var name: String,
    open var uniqueId: Int,

    open var eventType: EventType,
    open var startedAt: Long,

    open var endsAt: Long,

    open var description: MutableList<String>,
    open var spawnLocation: Location,

    open var regenerativeMaterialsHandler: RegenerativeMaterialsHandler,
    open var eventEntitiesHandler: EventEntitiesHandler,

    open var eventUserHandler: EventUserHandler,

    var requiredLevel: Int = 0,

    var portals: ConcurrentHashMap<EventPortalType, EventPortal>? = null,

    open var spawnRegion: Pair<Location, Location>? = null,

    open var questNPCLocation: Location? = null,

    open var quests: MutableList<Int>? = null,

    open var uuid: UUID? = null
) {

    open fun start() {
        uuid = UUID.randomUUID()
        eventUserHandler.setup(this)

        startedAt = System.currentTimeMillis()
        endsAt = getEndOfDayMillis(System.currentTimeMillis())

        portals?.values?.forEach { portal ->
            portal.setup()
        }
        spawnQuestNPC()

        eventEntitiesHandler.setup(this)
        regenerativeMaterialsHandler.setup(this)
    }
    open fun end() {
        finish()
        processFinishedEvent()

        portals?.values?.forEach { portal ->
            portal.end(PortalRemovalReason.END)
        }

        removeNPC()
        removeQuestNPCHologram()

        eventEntitiesHandler.stop()
        regenerativeMaterialsHandler.stop()
        eventUserHandler.end()
    }
    /*
    Statistics:
    All Points Earned
    Amount of players that participated
    Amount of islands that participated
    Mobs Killed
    Blocks Mined
    Quests finished

     */
    fun processFinishedEvent() {
        val eventsManager = EventsManager.instance
        val finishedQuest = FinishedEvent(eventsManager.finishedEvents.maxOfOrNull { it.key } ?: 1,
            uuid ?: UUID.randomUUID(), eventUserHandler.pointsHandler.islandsLeaderboard,
            eventUserHandler.users.values.map { it.toData() }.associateBy { it.uniqueId }, startedAt, System.currentTimeMillis())

        eventsManager.finishedEvents.putIfAbsent(finishedQuest.id, finishedQuest)
    }
    open fun finish() {
        val topPlayers = eventUserHandler.pointsHandler.getTopPlayers().take(3)

        val loreLines = buildList {
            topPlayers.forEach { (user, data) ->

                val rank = data.getOrNull(0) ?: 0
                val points = data.getOrNull(1) ?: 0
                val color = getRankColor(rank)

                add("$color$rank&7. $color${user.name} &8- &7$points Punktów.")
            }
        }
        val topIslandData: Pair<Island, Array<Int>>? = eventUserHandler.pointsHandler.getTopIslands().firstOrNull()

        val topIslandString: String? = topIslandData?.let { (island, data) ->
            val islandPoints = data.getOrNull(1) ?: 0
            "&7Najlepsza Wyspa: &f${island.name}&7, łącznie punktów: &e$islandPoints&7!"
        }
        val statisticsMessage = buildList {
            EventStatistic.entries.forEach {
                val totalSum = eventUserHandler.getTotalSumFor(it)
                if(totalSum == 0) return@forEach

                val topSum = eventUserHandler.getTopSumFor(it)
                val topSumName = topSum?.asSkyBlockUser()?.name
                when(it) {
                    EventStatistic.MOBS_KILLED -> {
                        add("&e&lZabitych Potworów&7: &6$totalSum")
                        if(topSum != null) add("&eNajlepszy Gracz&7: &6${topSumName}, ${topSum.mobsKilled} zabitych potworów.")
                    }
                    EventStatistic.BLOCKS_MINED -> {
                        add("&e&lWykopanych Bloków&7: &6$totalSum")
                        if(topSum != null) add("&eNajlepszy Gracz&7: &6${topSumName}, ${topSum.blocksMined} wykopanych bloków.")
                    }
                    EventStatistic.POINTS_EARNED -> {
                        add("&e&lZdobytych Punktów&7: &6$totalSum")
                        if(topSum != null) add("&eNajlepszy Gracz&7: &6${topSumName}, ${topSum.getPoints()} zdobytych punktów.")
                    }
                    EventStatistic.QUESTS_FINISHED -> {
                        add("&e&lUkończonych Zadań&7: &6$totalSum")
                        if(topSum != null) add("&eNajlepszy Gracz&7: &6${topSumName}, ${topSum.questsFinished.sum()} ukończonych zadań.")
                    }
                    EventStatistic.ISLANDS_PARTICIPATED -> add("&cWysp uczestniczących w wydarzeniu: &6$totalSum")
                    EventStatistic.PLAYERS_PARTICIPATED -> add("&cGraczy uczestniczących w wydarzeniu: &6$totalSum")
                }
            }
        }
        val finalMessage = buildList {
            add("&6&lWydarzenie Nether Zakończone!")
            add("&7-------------------------")
            if (loreLines.isNotEmpty()) {
                addAll(loreLines)
            } else {
                add("&7Brak graczy w rankingu.")
            }
            add("&7-------------------------")
            topIslandString?.let { add(it) } ?: add("&7Brak danych o najlepszej wyspie.")
            add("&7-------------------------")
            if(statisticsMessage.isNotEmpty()) {
                addAll(statisticsMessage)
            } else {
                add("&7Brak statystyk w wydarzeniu")
            }
        }

        Bukkit.getOnlinePlayers().forEach { player ->
            finalMessage.forEach { line ->
                player.sendColoured(line)
            }
        }
    }

    open fun reload() {
        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable {
            portals?.values?.forEach { portal ->
                portal.event = this
                portal.end(PortalRemovalReason.RESTART)

                portal.setup()
            }

            removeQuestNPCHologram()

            eventEntitiesHandler.reload(this)
            regenerativeMaterialsHandler.reload(this)
            eventUserHandler.reload(this)

            spawnQuestNPC()
        }, 30)
        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable {
            finish()
        }, 60)
    }
    open fun startMessage(): List<String> = emptyList()
    open fun joinMessage(): List<String> = emptyList()

    open fun addQuest(id: Int) {
        if(quests == null) quests = mutableListOf()
        if(quests!!.contains(id)) return
        quests!!.add(id)
    }
    open fun removeQuest(id: Int) {
        if(quests == null) quests = mutableListOf()
        if(!quests!!.contains(id))return

        Bukkit.getOnlinePlayers().map { UsersManager.instance.getUser(it.uniqueId, QuestUserLoadReason.DATA_RETRIEVAL) }.forEach { user ->
            if(user != null) {
                user.activeQuests.removeIf { it.uniqueId == id }
                user.givenQuests.remove(id)
                user.finishedQuests.remove(id)
            }
        }

        quests!!.remove(id)
    }

    fun spawnQuestNPC() {
        if (questNPCLocation == null) {
            return
        }
        if (FancyNpcsPlugin.get().npcManager.getNpc("${uniqueId}_event_npc") != null) {
            createQuestNPCHologram()
            return
        }
        val npcData = NpcData("${uniqueId}_event_npc", null, questNPCLocation)
        npcData.isGlowing = true
        npcData.isShowInTab = false

        npcData.isCollidable = false
        npcData.isTurnToPlayer = true

        npcData.interactionCooldown = 5F
        npcData.displayName = "<empty>"

        val npc = FancyNpcsPlugin.get().npcAdapter.apply(npcData)
        val npcManager = FancyNpcsPlugin.get().npcManager

        npcManager.registerNpc(npc)
        npc.data.setSkin("https://imgur.com/DxhxnRi")

        npc.create()
        npc.spawnForAll()
        npc.updateForAll()

        createQuestNPCHologram()
    }
    private fun createQuestNPCHologram() {
        try {

            val npcManager = FancyNpcsPlugin.get().npcManager
            val npc = npcManager.getNpc("${uniqueId}_event_npc")
            if(npc == null) {
                return
            }
            val hologramManager = FancyHologramsPlugin.get().hologramManager

            val hologramLocation = npc.data.location.clone().add(.0, npc.eyeHeight + 0.5, .0)
            SkyBlockEventsResourcesManager.instance.languages.forEach { locale ->
                val lore = listOf(
                    "devil".localeEventsTranslation(locale),
                    "click.to.talk".localeEventsTranslation(locale)
                )
                val hologram = hologramManager.getHologram("${uniqueId}_event_npc_hologram_$locale").orElse(null)
                if (hologram != null) return

                val hologramData = TextHologramData("${uniqueId}_event_npc_hologram_$locale", hologramLocation)
                hologramData.text = lore

                hologramData.background = Hologram.TRANSPARENT
                hologramData.linkedNpcName = "${uniqueId}_event_npc"
                hologramData.visibility = Visibility.MANUAL

                hologramManager.addHologram(hologramManager.create(hologramData))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // NPC
    fun removeNPC() {
        try {
            if(questNPCLocation == null) return

            val npc = FancyNpcsPlugin.get().npcManager.getNpc("${uniqueId}_event_npc")
            if(npc == null) return

            npc.removeForAll()
            npc.updateForAll()
            FancyNpcsPlugin.get().npcManager.removeNpc(npc)
            FancyNpcsPlugin.get().npcManager.reloadNpcs()

            removeQuestNPCHologram()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun reloadNPC() {
        try {
            val npc = FancyNpcsPlugin.get().npcManager.getNpc("${uniqueId}_event_npc")

            if (npc == null) {
                spawnQuestNPC()
                return
            } else {
                removeNPC()
                spawnQuestNPC()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Holograms

    private fun removeQuestNPCHologram() {
        try {
            val hologramManager = FancyHologramsPlugin.get().hologramManager
            SkyBlockEventsResourcesManager.instance.languages.forEach { locale ->

                val hologram = hologramManager.getHologram("${uniqueId}_event_npc_hologram_$locale").orElse(null)
                if (hologram != null) hologramManager.removeHologram(hologram)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun showNPCHologram(player: Player, hologramManager: HologramManager, skyBlockEventsResourcesManager: SkyBlockEventsResourcesManager) {
        val locale = if(!skyBlockEventsResourcesManager.languages.contains(player.locale)) "en_us" else player.locale
        val hologramName = "${uniqueId}_event_npc_hologram_$locale"

        val hologram = hologramManager.getHologram(hologramName).getOrNull() ?: return

        if(!hologram.isWithinVisibilityDistance(player)) return

        if(!Visibility.ManualVisibility.canSee(player, hologram)) {

            Visibility.ManualVisibility.addDistantViewer(hologram, player.uniqueId)
            hologram.forceShowHologram(player)
        }
    }
    fun removeDistantViewerFromNPCHologram(player: Player, skyBlockEventsResourcesManager: SkyBlockEventsResourcesManager, hologramManager: HologramManager) {
        val hologramNames = mutableListOf<String>()
        val languages = skyBlockEventsResourcesManager.languages

        val playerLocale = if(languages.contains(player.locale)) player.locale else "en_us"

        languages.forEach { locale ->
            if(locale == playerLocale) return@forEach

            hologramNames.add("${uniqueId}_event_npc_hologram_$locale")
        }

        val holograms = hologramNames.map { hologramManager.getHologram(it).getOrNull() }
        holograms.removeViewer(player)
    }

    // Regions
    fun isInSpawnRegion(location: Location): Boolean {
        if(spawnRegion == null) return false
        return location.isInCuboid(spawnRegion!!)
    }
    fun getPortalAt(location: Location): EventPortal? =
        portals?.values?.firstOrNull {
            it.isIn(location)
        }
    fun normalPortalLocation(): Location? = portals?.values?.firstOrNull { it.portalType == EventPortalType.NORMAL }?.centre

    fun toData() = EventData(
        name,
        uniqueId,
        eventType,
        startedAt,
        endsAt,
        description,
        spawnLocation,
        requiredLevel,
        portals,
        spawnRegion,
        questNPCLocation,
        quests,
        eventEntitiesHandler.cuboids,
        regenerativeMaterialsHandler.regenerativeMaterials,
        eventUserHandler.users.mapValuesTo(ConcurrentHashMap()) { it.value.toData() },
        uuid
    )

}