package com.justxraf.skyblockevents.commands.players

import com.justxdude.islandcore.utils.toLocationString
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.commands.Command
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.networkapi.util.Utils.toDate
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.eventsTranslation
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EventInfoCommand : Command("wydarzenie", arrayOf("wydarzenia", "wyd", "event", "events")) {
    private val eventsManager = EventsManager.instance

    override fun canExecute(player: Player, args: Array<String>): Boolean = true

    override fun execute(player: Player, args: Array<String>) {
        val currentEvent = eventsManager.currentEvent
        val joined = currentEvent.playersWhoJoined.size
        val activePlayers = currentEvent.activePlayers.size
        val message = mutableListOf<String>()

        val user = player.asUser() ?: return

        message += listOf(
            "${if(user.level < currentEvent.requiredLevel) "&c" else "&9" }&m-".repeat(30),
            "events.information.title".eventsTranslation(player),
            "&7"
        )
        if(currentEvent.description.isNotEmpty())
            message += listOf(
                *currentEvent.description.map { it.eventsTranslation(player) }.toTypedArray(),
                "&7"
            )

        message += listOf(
            "&8- &7${if(joined == 0) "nobody.joined".eventsTranslation(player) else if(joined == 1) "joined.one.player".eventsTranslation(player) else "joined.in.total".eventsTranslation(player, joined.toString())}",
            "&8- &7${if(activePlayers == 0) "event.no.active.players".eventsTranslation(player)
                else if(activePlayers == 1) "active.one.player".eventsTranslation(player)
                    else "active.in.total".eventsTranslation(player, activePlayers.toString())}",

            "ends.in".eventsTranslation(player, currentEvent.endsAt.toDate()),
            "&c",
            if(user.level < currentEvent.requiredLevel) "have.to.achieve.level.to.unlock" else "join.at"
                .eventsTranslation(player, currentEvent.normalPortalLocation()?.toLocationString() ?: ""),
            "&c",
            "${if(user.level < currentEvent.requiredLevel) "&c" else "&9" }&m-".repeat(30),
        )

        message.forEach {
            player.sendColoured(it)
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()

}