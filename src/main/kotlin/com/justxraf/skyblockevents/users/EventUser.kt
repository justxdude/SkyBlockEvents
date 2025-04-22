package com.justxraf.skyblockevents.users

import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.user.User
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserManager
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.api.SkyBlockEventPointsGainEvent
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.user.EventUserData
import com.justxraf.skyblockevents.util.eventsTranslation
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID

/*
TODO
- User is only added to the event once it joins it
- User is removed from the event only once the event ends
- User holds statistics like points, mobs killed, blocks mined, quests finished
- Add "isActive" (copy from activity checker from the event)

 */

class EventUser(
    val uniqueId: UUID,
    private var points: Int = 0,

    var mobsKilled: Int = 0,
    var blocksMined: Int = 0,

    var questsFinished: MutableList<Int> = mutableListOf(),
    var isActive: Boolean = true,

    var lastCache: Long = System.currentTimeMillis()
) {
    var player: Player? = null
    fun asSkyBlockUser(): User? = UserManager.instance.getUser(uniqueId)

    fun shouldKick(world: World): Boolean {
        val delay = 300 * 1000 // 5 minutes
        return lastCache + delay < System.currentTimeMillis() || world != player?.world
    }
    fun kick(world: World) {
        player?.let { player ->
            if (world != player.world) return

            isActive = false
            SkyblockAPI.instance.spawn.teleport(player, false)

            player.sendColoured("no.activity".eventsTranslation(player))
            this.player = null
        }
    }
    fun addPoints(amount: Int) {
        val currentEvent = EventsManager.instance.currentEvent
        points += amount

        Bukkit.getPluginManager().callEvent(SkyBlockEventPointsGainEvent(uniqueId.asUser()!!, amount, currentEvent.eventType))


    }
    fun getPoints() = points
    fun toData(): EventUserData = EventUserData(uniqueId, points, mobsKilled, blocksMined, questsFinished, isActive, lastCache)
}