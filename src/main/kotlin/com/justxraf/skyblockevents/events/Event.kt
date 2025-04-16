package com.justxraf.skyblockevents.events

import com.justxdude.skyblockapi.rewards.ItemReward
import com.justxdude.skyblockapi.rewards.MoneyReward
import com.justxdude.skyblockapi.rewards.Reward
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserSettingsFlag
import com.justxraf.networkapi.util.asAudience
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.networkapi.util.sendColouredActionBar
import com.justxraf.questscore.api.UserQuestCancelEvent
import com.justxraf.questscore.users.QuestUser
import com.justxraf.questscore.users.QuestUserLoadReason
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.events.event.EventEntitiesHandler
import com.justxraf.skyblockevents.events.player.EventPlayer
import com.justxraf.skyblockevents.events.points.PointsHandler
import com.justxraf.skyblockevents.events.portals.EventPortal
import com.justxraf.skyblockevents.events.portals.EventPortalType
import com.justxraf.skyblockevents.events.portals.PortalRemovalReason
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterialsHandler
import com.justxraf.skyblockevents.translations.SkyBlockEventsResourcesManager
import com.justxraf.skyblockevents.util.eventsTranslation
import com.justxraf.skyblockevents.util.getEndOfDayMillis
import com.justxraf.skyblockevents.util.isInCuboid
import com.justxraf.skyblockevents.util.localeEventsTranslation
import com.justxraf.skyblockevents.util.removeViewer
import com.justxraf.skyblockevents.util.translateComponentWithClickEvent
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.HologramManager
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.data.property.Visibility
import de.oliver.fancyholograms.api.hologram.Hologram
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.NpcData
import gg.flyte.twilight.builders.item.ItemBuilder
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*
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

    open var pointsHandler: PointsHandler,

    var requiredLevel: Int = 0,

    var portals: MutableMap<EventPortalType, EventPortal>? = null,

    open var spawnRegion: Pair<Location, Location>? = null,

    open var questNPCLocation: Location? = null,

    open var quests: MutableList<Int>? = null,
    open val playersWhoJoined: MutableList<UUID> = mutableListOf(),

    open var uuid: UUID? = null
) {
    @delegate:Transient
    private val components by lazy { ComponentsManager.instance }
    @Transient lateinit var activePlayers: MutableMap<UUID, EventPlayer>
    @Transient var disabledNotifications: MutableList<UUID> = mutableListOf()
    @Transient private var notificationsTask: BukkitTask? = null
    @Transient private var activityCheckTask: BukkitTask? = null

    open fun start() {
        uuid = UUID.randomUUID()

        activePlayers = mutableMapOf()
        disabledNotifications = mutableListOf()

        playersWhoJoined.clear()
        clearPlayersQuests()

        startedAt = System.currentTimeMillis()
        endsAt = getEndOfDayMillis(System.currentTimeMillis())

        portals?.values?.forEach { portal ->
            portal.setup()
        }
        spawnQuestNPC()

        runTasks()

        eventEntitiesHandler.setup(this)
        regenerativeMaterialsHandler.setup(this)

        pointsHandler.setup(this)
        pointsHandler.players.clear()

    }
    open fun end() {
        clearPlayersQuests()

        portals?.values?.forEach { portal ->
            portal.end(PortalRemovalReason.END)
        }

        removeNPC()
        removeQuestNPCHologram()

        activityCheckTask?.cancel()
        notificationsTask?.cancel()

        eventEntitiesHandler.stop()
        regenerativeMaterialsHandler.stop()
        pointsHandler.stop()
        pointsHandler.players.clear()

        playersWhoJoined.clear()
    }

    open fun giveRewards() {
        val rewardsFirst = listOf<Reward>(
            MoneyReward(10000.00),
            ItemReward(listOf(ItemBuilder(Material.GLOWSTONE).build()))
        )

        rewardsFirst.forEach {
            it.sendRewardToUser()
        }
    }

    open fun reload() {
        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable {
            disabledNotifications = mutableListOf()
            activePlayers = mutableMapOf()

            runTasks()
            checkActivePlayers()

            portals?.values?.forEach { portal ->
                portal.event = this
                portal.end(PortalRemovalReason.RESTART)

                portal.setup()
            }

            removeQuestNPCHologram()

            eventEntitiesHandler.reload(this)
            regenerativeMaterialsHandler.reload(this)
            pointsHandler.reload(this)

            spawnQuestNPC()
        }, 30)
    }
    open fun startMessage(): List<String> = emptyList()
    open fun joinMessage(): List<String> = emptyList()
    open fun runTasks() {
        activityCheckTask = object : BukkitRunnable() {
            override fun run() {
                checkActivePlayers()
            }
        }.runTaskTimer(components.plugin, 0, 20 * 10) // Check every 10 seconds.
        notificationsTask = object : BukkitRunnable() {
            override fun run() {
                sendNotification()
            }
        }.runTaskTimer(components.plugin, 0, 20 * 540) // every 5 minutes 540
    }
    private fun sendNotification() {
        Bukkit.getOnlinePlayers().filter {
            val user = it.asUser()
            user != null
                    && it.world != spawnLocation.world
                    && user.level >= requiredLevel
                    && user.getFlagBoolean(UserSettingsFlag.ALLOW_EVENT_NOTIFICATIONS)
                    && !disabledNotifications.contains(it.uniqueId)
        }.forEach { player ->
            if(disabledNotifications.contains(player.uniqueId)) return@forEach

            player.asAudience().sendMessage("event.is.on".translateComponentWithClickEvent(player,
                "disable_event_notification", "event.is.on.click".eventsTranslation(player), name.lowercase().eventsTranslation(player)))
        }
    }
    private fun clearPlayersQuests() { // Removes the same quests which were finished previously.
        val availableQuests = quests ?: return

        val usersManager = UsersManager.instance
        val questUsers = Bukkit.getOnlinePlayers().mapNotNull { usersManager.getUser(it.uniqueId, QuestUserLoadReason.DATA_RETRIEVAL) }

        questUsers.forEach { questUser ->
            val keysToRemove =
                questUser.finishedQuests.filter { availableQuests.contains(it.key) && it.value.time < startedAt }
                    .map { it.key }

            keysToRemove.forEach { questUser.finishedQuests.remove(it) }

            val activeQuests = questUser.activeQuests.filter { availableQuests.contains(it.uniqueId) }

            activeQuests.forEach { quest -> questUser.activeQuests.remove(quest) }

        }
    }
    open fun teleport(player: Player) {
        player.teleport(spawnLocation)
        if(!playersWhoJoined.contains(player.uniqueId)) {
            player.sendColouredActionBar("joined.event".eventsTranslation(player, name))
            playersWhoJoined.add(player.uniqueId)
            if(description.size != 1)
                description.forEach { player.sendColoured(it) }
        } else {
            player.sendColouredActionBar("teleported.event".eventsTranslation(player, name))
        }
    }
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

    // Activity
    private fun checkActivePlayers() {
        val world = spawnLocation.world ?: return
        val playersToRemove = mutableListOf<UUID>()

        activePlayers.forEach { (uuid, eventPlayer) ->
            if(eventPlayer.shouldRemove(world)) {
                playersToRemove.add(uuid)
                eventPlayer.kick(world)
            }
        }
        playersToRemove.forEach {
            activePlayers.remove(it)
        }

        val players: Map<UUID, EventPlayer> = Bukkit.getOnlinePlayers()
            .filter { it.uniqueId !in activePlayers && it.world == world }
            .associateBy({ it.uniqueId }, { EventPlayer(it, System.currentTimeMillis()) })

        activePlayers.putAll(players)
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

    fun restartQuestsFor(questUser: QuestUser) {
        if(quests.isNullOrEmpty()) quests = mutableListOf()
        if(playersWhoJoined.contains(questUser.uniqueId)) return

        questUser.finishedQuests.filter { quests!!.contains(it.key) }.forEach {
            questUser.finishedQuests.remove(it.key)
        }
        questUser.activeQuests.filter { quests!!.contains(it.uniqueId) }.forEach {
            questUser.activeQuests.remove(it)

            val user = questUser.uniqueId.asUser() ?: return@forEach
            Bukkit.getPluginManager().callEvent(UserQuestCancelEvent(user, questUser, it))
        }
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
        playersWhoJoined,
        eventEntitiesHandler.cuboids,
        regenerativeMaterialsHandler.regenerativeMaterials,
        pointsHandler.players,
        uuid
    )

}