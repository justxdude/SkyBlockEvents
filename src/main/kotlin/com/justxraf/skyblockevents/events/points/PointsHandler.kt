package com.justxraf.skyblockevents.events.points

import com.github.supergluelib.foundation.util.ItemBuilder
import com.justxdude.islandcore.islands.Island
import com.justxdude.islandcore.islands.islandmanager.IslandManager
import com.justxdude.skyblockapi.user.User
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.components.ComponentsManager
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

class PointsHandler(
    /*
    todo Make sure to handle everything in event class and save in eventdata---
    todo Make a class extending Reward to add points for quests.

     */


    private var players: MutableMap<UUID, Int>,
    private var islands: MutableMap<Int, Int> // uuid, amount
) {
    private var islandChecker: BukkitTask? = null
    lateinit var islandsLeaderboard: List<Pair<Int, Int>>
    lateinit var playersLeaderboard: List<Pair<UUID, Int>>

    var lastLeaderboardUpdate: Long = System.currentTimeMillis()

    /*

    Update maps containing islands every 10 seconds

     */

    fun initialize() {
        startTasks()
    }
    fun stop() {
        stopTasks()
    }
    private fun startTasks() {
        islandChecker = object : BukkitRunnable() {
            override fun run() {
                islandsCheck()
                updateLeaderboard()
            }
        }.runTaskTimer(ComponentsManager.instance.plugin, 0, 20 * 10) // Check every 5 seconds.
    }
    private fun stopTasks() {
        islandChecker?.cancel()
    }
    private fun islandsCheck() {
        val users = players.mapNotNull { (uuid, _) -> uuid.asUser() }

        val newKeys = users.filter { it.islandId != 0 }.map { it.islandId }.distinct()

        newKeys.forEach { islandId ->
            val island = IslandManager.instance.getIslandViaId(islandId) ?: return@forEach
            val islandMembers = players.filter { island.members.contains(it.key) }

            val total = islandMembers.values.sumOf { it }

            islands[islandId] = total
        }
    }
    private fun updateLeaderboard() {
        lastLeaderboardUpdate = System.currentTimeMillis()
        playersLeaderboard = players.toList().sortedByDescending { it.second }
        islandsLeaderboard = islands.toList().sortedByDescending { it.second }
    }

    fun addPoints(player: UUID, amount: Int) {
        val currentAmount = players.getOrPut(player) { 0 }

        players[player] = currentAmount + amount
    }
    fun getIslandPosition(id: Int): Int
        = islandsLeaderboard.indexOfFirst { it.first == id }
    fun getPlayerPosition(uuid: UUID): Int
        = playersLeaderboard.indexOfFirst { it.first == uuid }
    fun getTopIslands(): List<Pair<Island, Array<Int>>> {
        val topIslands = islandsLeaderboard.take(10)
        var pos = 1
        val list: MutableList<Pair<Island, Array<Int>>> = mutableListOf()

        topIslands.forEach { (id, points) ->
            // Island > Position > Points
            val island = IslandManager.instance.getIslandViaId(id) ?: return@forEach
            list += Pair(island, arrayOf(pos, points))
           pos++
        }
        return list.sortedByDescending { it.second[0] }
    }
    fun getTopPlayers(): List<Pair<User, Array<Int>>> {
        val topPlayers = playersLeaderboard.take(10)
        var pos = 1
        val list: MutableList<Pair<User, Array<Int>>> = mutableListOf()

        topPlayers.forEach { (id, points) ->
            // Island > Position > Points
            val user = id.asUser() ?: return@forEach
            list += Pair(user, arrayOf(pos, points))
            pos++
        }
        return list.sortedByDescending { it.second[0] }
    }

}