package com.justxraf.skyblockevents.users

import com.justxdude.islandcore.rewards.data.EntitySpawnRewardData
import com.justxdude.islandcore.rewards.data.IslandItemRewardData
import com.justxdude.islandcore.rewards.data.IslandMoneyRewardData
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.rewards.RewardsHandler
import com.justxdude.skyblockapi.rewards.data.ItemRewardData
import com.justxdude.skyblockapi.rewards.data.MoneyRewardData
import com.justxdude.skyblockapi.rewards.data.XpRewardData
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserSettingsFlag
import com.justxraf.networkapi.util.asAudience
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.networkapi.util.sendColouredActionBar
import com.justxraf.questscore.api.UserQuestCancelEvent
import com.justxraf.questscore.users.QuestUser
import com.justxraf.questscore.users.QuestUserLoadReason
import com.justxraf.questscore.users.UsersManager
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.users.points.PointsHandler
import com.justxraf.skyblockevents.util.eventsTranslation
import com.justxraf.skyblockevents.util.toItemStack
import com.justxraf.skyblockevents.util.translateComponentWithClickEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EventUserHandler(
    var pointsHandler: PointsHandler,
    val users: ConcurrentHashMap<UUID, EventUser> = ConcurrentHashMap(),
) {
    lateinit var event: Event
    var disabledNotifications: MutableList<UUID> = mutableListOf()

    private var notificationsTask: BukkitTask? = null
    private var activityCheckTask: BukkitTask? = null

    lateinit var playerRewards: Map<Int, List<Int>>
    lateinit var islandRewards: List<Int>

    private val components by lazy { ComponentsManager.instance }

    fun setup(event: Event) {
        this.event = event
        pointsHandler.setup(this)

        clearUsersQuests()

        stopTasks()
        runTasks()

        loadRewards()
        disabledNotifications = mutableListOf()
    }

    fun reload(event: Event) {
        this.event = event

        pointsHandler.reload(this)
        loadRewards()
        stopTasks()
        runTasks()

        disabledNotifications = mutableListOf()
    }

    fun end() {
        clearUsersQuests()
        teleportActiveUsers()
        giveRewards()


        // Clearing logic.
        users.clear()

        disabledNotifications.clear()
        stopTasks()

        playerRewards = mapOf()
        islandRewards = listOf()

        pointsHandler.stop()
    }

    private fun teleportActiveUsers() {
        users.filter { it.value.isActive }.forEach { (uuid, user) ->
            user.player?.sendColoured("&7Zostałeś(aś) przeteleportowany(a) z obecnego wydarzenia na spawn, ponieważ się ono zakończyło.")
            user.player?.teleport(SkyblockAPI.instance.spawn.location)
        }
    }

    private fun loadRewards() {
        val rewardsHandler = RewardsHandler.instance
        playerRewards = hashMapOf(
            1 to rewardsHandler.addRewards(
                listOf(
                    ItemRewardData(
                        listOf(
                            Material.NETHERITE_INGOT.toItemStack(8),
                            Material.GLOWSTONE.toItemStack(64),
                            Material.IRON_INGOT.toItemStack(32),
                            Material.DIAMOND.toItemStack(16)
                        )
                    ),
                    MoneyRewardData(25000.00),
                    XpRewardData(5000)
                )
            ),
            2 to rewardsHandler.addRewards(
                listOf(
                    ItemRewardData(
                        listOf(
                            Material.NETHERITE_INGOT.toItemStack(4),
                            Material.GLOWSTONE.toItemStack(64),
                            Material.IRON_INGOT.toItemStack(16),
                            Material.DIAMOND.toItemStack(8)
                        )
                    ),
                    MoneyRewardData(12500.00),
                    XpRewardData(2500)
                )
            ),
            3 to rewardsHandler.addRewards(
                listOf(
                    ItemRewardData(
                        listOf(
                            Material.NETHERITE_INGOT.toItemStack(2),
                            Material.GLOWSTONE.toItemStack(32),
                            Material.IRON_INGOT.toItemStack(8),
                            Material.DIAMOND.toItemStack(4)
                        )
                    ),
                    MoneyRewardData(5000.00),
                    XpRewardData(500)
                )
            )
        )
        islandRewards = rewardsHandler.addRewards(
            listOf(
                EntitySpawnRewardData(3, EntityType.CAMEL),
                IslandItemRewardData(128, Material.DIAMOND),
                IslandItemRewardData(64, Material.OBSIDIAN),
                IslandItemRewardData(8, Material.NETHERITE_INGOT),
                IslandMoneyRewardData(50000.0)
            )
        )
    }

    private fun giveRewards() {
        val rewardsHandler = RewardsHandler.instance

        val topPlayers = pointsHandler.getTopPlayers().take(3)

        topPlayers.forEach { (user, array) ->
            val rewards = playerRewards[array[0]]?.mapNotNull { rewardsHandler.getReward(it) } ?: return@forEach
            rewards.forEach { it.sendRewardToUser(user, true) }
        }
        val (island, _) = pointsHandler.getTopIslands().take(1)[0]
        islandRewards.mapNotNull { rewardsHandler.getReward(it) }.forEach {
            it.sendRewardToUser(island.owner.asUser()!!, true)
        }
    }

    private fun stopTasks() {
        activityCheckTask?.cancel()
        notificationsTask?.cancel()
    }

    fun runTasks() {
        activityCheckTask = object : BukkitRunnable() {
            override fun run() {
                checkUsers()
            }
        }.runTaskTimer(components.plugin, 0, 20) // Check every 10 seconds.
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
                    && it.world != event.spawnLocation.world
                    && user.level >= event.requiredLevel
                    && user.getFlagBoolean(UserSettingsFlag.ALLOW_EVENT_NOTIFICATIONS)
                    && !disabledNotifications.contains(it.uniqueId)
        }.forEach { player ->
            if (disabledNotifications.contains(player.uniqueId)) return@forEach

            player.asAudience().sendMessage(
                "event.is.on".translateComponentWithClickEvent(
                    player,
                    "disable_event_notification",
                    "event.is.on.click".eventsTranslation(player),
                    event.name.lowercase().eventsTranslation(player)
                )
            )
        }
    }

    private fun checkUsers() {
        val world = event.spawnLocation.world ?: return

        users.filter { (_, eventUser) -> eventUser.shouldKick(world) }
            .forEach { (_, eventUser) ->
                eventUser.kick(world)
            }

        val onlinePlayerUUIDs = Bukkit.getOnlinePlayers()
            .filter { it.world == world }
            .map { it.uniqueId }
            .toSet()

        // Update active status
        users.forEach { (uuid, user) ->
            user.isActive = onlinePlayerUUIDs.contains(uuid)
            if (user.isActive) {
                user.player = Bukkit.getPlayer(uuid)
            } else {
                user.player = null
            }
        }

        // Optionally handle players online but not in users (potential error case)
        onlinePlayerUUIDs.forEach { uniqueId ->
            if (!users.containsKey(uniqueId)) {
                println("WARNING: Player $uniqueId is online in the event world but not tracked in users!")
                users.putIfAbsent(uniqueId, EventUser(uniqueId)) // Or handle differently
            }

            users.filter { it.value.isActive }.forEach { it.value.player = Bukkit.getPlayer(it.key) }
        }
    }
    fun restartQuestsFor(questUser: QuestUser) {
        if(event.quests.isNullOrEmpty()) event.quests = mutableListOf()
        if(users.contains(questUser.uniqueId)) return

        questUser.finishedQuests.filter { event.quests!!.contains(it.key) }.forEach {
            questUser.finishedQuests.remove(it.key)
        }
        questUser.activeQuests.filter { event.quests!!.contains(it.uniqueId) }.forEach {
            questUser.activeQuests.remove(it)

            val user = questUser.uniqueId.asUser() ?: return@forEach
            Bukkit.getPluginManager().callEvent(UserQuestCancelEvent(user, questUser, it))
        }
    }

    private fun clearUsersQuests() { // Removes the same quests which were finished previously.
        val availableQuests = event.quests ?: return

        val usersManager = UsersManager.instance
        val questUsers = users.mapNotNull { usersManager.getUser(it.key, QuestUserLoadReason.DATA_RETRIEVAL) }

        questUsers.forEach { questUser ->
            val keysToRemove =
                questUser.finishedQuests.filter { availableQuests.contains(it.key) && it.value.time < event.startedAt }
                    .map { it.key }

            keysToRemove.forEach { questUser.finishedQuests.remove(it) }

            val activeQuests = questUser.activeQuests.filter { availableQuests.contains(it.uniqueId) }
            activeQuests.forEach { quest -> questUser.activeQuests.remove(quest) }
        }
    }
    fun getTotalSumFor(statistic: EventStatistic): Int =
        when(statistic) {
            EventStatistic.MOBS_KILLED -> users.entries.sumOf { it.value.mobsKilled }
            EventStatistic.BLOCKS_MINED -> users.entries.sumOf { it.value.blocksMined }
            EventStatistic.POINTS_EARNED -> users.entries.sumOf { it.value.getPoints() }
            EventStatistic.QUESTS_FINISHED -> users.entries.sumOf { it.value.questsFinished.sum() }
            EventStatistic.ISLANDS_PARTICIPATED -> pointsHandler.islands.size
            EventStatistic.PLAYERS_PARTICIPATED -> users.size
        }

    fun getTopSumFor(statistic: EventStatistic): EventUser? =
        users.values.maxByOrNull { user ->
            when (statistic) {
                EventStatistic.MOBS_KILLED -> user.mobsKilled
                EventStatistic.BLOCKS_MINED -> user.blocksMined
                EventStatistic.POINTS_EARNED -> user.getPoints()
                EventStatistic.QUESTS_FINISHED -> user.questsFinished.sum()
                else -> 0
            }
        }

    fun getUser(key: UUID): EventUser {
        var user = users[key]
        if(user == null) {
            println("User is null, creating new one!")
            user = EventUser(key)
            users[key] = user
        }
        return user
    }

    fun teleport(player: Player) {
        player.teleport(event.spawnLocation)
        if(!users.contains(player.uniqueId)) {
            player.sendColouredActionBar("joined.event".eventsTranslation(player, event.name))
            val u = getUser(player.uniqueId)
            if(event.description.size != 1)
                event.description.forEach { player.sendColoured(it) }
        } else {
            player.sendColouredActionBar("teleported.event".eventsTranslation(player, event.name))
        }
    }
}