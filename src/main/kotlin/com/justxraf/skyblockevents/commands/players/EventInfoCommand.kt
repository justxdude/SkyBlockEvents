package com.justxraf.skyblockevents.commands.players

import com.justxdude.islandcore.utils.toLocationString
import com.justxdude.skyblockapi.user.UserExtensions.asPlayer
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.commands.Command
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.networkapi.util.Utils.toDate
import com.justxraf.networkapi.util.asAudience
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.util.eventsTranslation
import com.justxraf.skyblockevents.util.translateComponentWithClickEvent
import gg.flyte.twilight.string.translate
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EventInfoCommand : Command("wydarzenie", arrayOf("wydarzenia", "wyd", "event", "events")) {
    private val eventsManager = EventsManager.instance

    override fun canExecute(player: Player, args: Array<String>): Boolean {
        if(args.isEmpty()) return true

        val usage = "event.info.usage".eventsTranslation(player)
        if(args.size != 1) {
            player.sendColoured("wrong.amount.of.arguments".eventsTranslation(player, usage))
            return false
        }
        val subCommands = listOf("leaderboard", "rewards")
        if(!subCommands.contains(args[0].eventsTranslation(player))) {
            player.sendColoured("wrong.usage".eventsTranslation(player, usage))
            return false
        }

        return true
    }

    override fun execute(player: Player, args: Array<String>) {
        if(args.size == 1) {
            when(args[0].eventsTranslation(player)) {
                "leaderboard" -> EventLeaderboardSubCommand.execute(player)
                "rewards" -> EventRewardsSubCommand.execute(player)
            }
            return
        }


        val currentEvent = eventsManager.currentEvent
        val joined = currentEvent.eventUserHandler.users.size
        val users = currentEvent.eventUserHandler.users.size
        val message = mutableListOf<String>()

        val user = player.asUser() ?: return

        message += listOf(
            "${if(user.level < currentEvent.requiredLevel) "&c" else "&9" }&m-".repeat(30),
            "events.information.title".eventsTranslation(player),
            "&7"
        )

        message += listOf(
            "&8- &7${if(joined == 0) "nobody.joined".eventsTranslation(player) else if(joined == 1) "joined.one.player".eventsTranslation(player) else "joined.in.total".eventsTranslation(player, joined.toString())}",
            "&8- &7${when(users) {
                1 -> "active.one.player".eventsTranslation(player)
                0 -> "event.no.active.players".eventsTranslation(player)
                else -> "active.in.total".eventsTranslation(player, users.toString())}
            }",
            "ends.in".eventsTranslation(player, currentEvent.endsAt.toDate()),
            "&c",
            if(user.level < currentEvent.requiredLevel) "have.to.achieve.level.to.unlock" else "join.at"
                .eventsTranslation(player, currentEvent.normalPortalLocation()?.toLocationString() ?: ""),
            "&c",
            "${if(user.level < currentEvent.requiredLevel) "&c" else "&9" }&m-".repeat(30),
        )
        var i = 1
        message.forEach {
            player.sendColoured(it)
            if(i == 6) player.asAudience().sendMessage {
                "check.leaderboard".translateComponentWithClickEvent(player, "event ${"leaderboard".eventsTranslation(player)}",
                "click.to.run.command".eventsTranslation(player)) }
            if(i == 7) player.asAudience().sendMessage(
                "check.rewards".translateComponentWithClickEvent(player, "event rewards",
                    "click.to.run.command".eventsTranslation(player))
            )
            i++
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if(args.size == 1) listOf(
            "leaderboard".eventsTranslation(sender as Player),
            "rewards".eventsTranslation(sender)
        ) else emptyList()

}