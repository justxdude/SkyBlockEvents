package com.justxraf.skyblockevents.users.points

import com.justxdude.islandcore.islands.Island
import com.justxdude.islandcore.islands.managers.IslandManager
import com.justxdude.skyblockapi.user.User
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.users.EventUserHandler
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PointsHandler() {
    lateinit var eventUserHandler: EventUserHandler
    lateinit var islands: ConcurrentHashMap<Int, Int>
    private var islandChecker: BukkitTask? = null
    lateinit var islandsLeaderboard: List<Pair<Int, Int>>
    lateinit var playersLeaderboard: List<Pair<UUID, Int>>

    var lastLeaderboardUpdate: Long = System.currentTimeMillis()

    fun setup(eventUserHandler: EventUserHandler) {
        this.eventUserHandler = eventUserHandler
        islands = ConcurrentHashMap()

        islandsLeaderboard = listOf()
        playersLeaderboard = listOf()

        startTasks()
    }
    fun stop() {
        stopTasks()
    }
    fun reload(eventUserHandler: EventUserHandler) {
        stop()

        setup(eventUserHandler)
    }
    private fun startTasks() {
        islandChecker = object : BukkitRunnable() {
            override fun run() {
                islandsCheck()
                updateLeaderboard()
            }
        }.runTaskTimer(ComponentsManager.instance.plugin, 0, 20 * 10) // Check every 10 seconds.
    }
    private fun stopTasks() {
        islandChecker?.cancel()
    }
    private fun islandsCheck() {
        val users = eventUserHandler.users.filter { it.value.getPoints() != 0 }.mapNotNull { (uuid, _) -> uuid.asUser() }
        if(users.isEmpty()) return

        val newKeys = users.filter { it.islandId != 0 }.map { it.islandId }.distinct()

        newKeys.forEach { islandId ->
            val island = IslandManager.instance.getIsland(islandId) ?: return@forEach
            val islandMembers = eventUserHandler.users.filter { island.memberHandler.isMember(it.key) }

            val total = islandMembers.values.sumOf { it.getPoints() }

            islands[islandId] = total
        }
    }
    private fun updateLeaderboard() {
        lastLeaderboardUpdate = System.currentTimeMillis()

        playersLeaderboard = eventUserHandler.users
            .filter { it.value.getPoints() != 0 }.map { Pair(it.value.uniqueId, it.value.getPoints()) }
            .sortedByDescending { it.second }
        islandsLeaderboard = islands.toList().sortedByDescending { it.second }
    }
    fun getIslandPosition(id: Int): Int
        = islandsLeaderboard.indexOfFirst { it.first == id } + 1
    fun getPlayerPosition(uuid: UUID): Int
        = playersLeaderboard.indexOfFirst { it.first == uuid } + 1


    fun getTopIslands(): List<Pair<Island, Array<Int>>> {
        val topIslands = islandsLeaderboard.take(10)
        var pos = 1
        val list: MutableList<Pair<Island, Array<Int>>> = mutableListOf()

        topIslands.forEach { (id, points) ->
            // Island > Position > Points
            val island = IslandManager.instance.getIsland(id) ?: return@forEach
            list += Pair(island, arrayOf(pos, points))
           pos++
        }
        return list.sortedBy { it.second[0] }
    }
    fun getTopPlayers(): List<Pair<User, Array<Int>>> {
        if(playersLeaderboard.isNullOrEmpty()) return listOf()

        val topPlayers = playersLeaderboard.take(10)
        var pos = 1
        val list: MutableList<Pair<User, Array<Int>>> = mutableListOf()

        topPlayers.forEach { (id, points) ->
            // Island > Position > Points
            val user = id.asUser() ?: return@forEach
            list += Pair(user, arrayOf(pos, points))
            pos++
        }
        return list.sortedBy { it.second[0] }
    }

}