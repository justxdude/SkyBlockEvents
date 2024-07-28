package com.justxraf.skyblockevents.commands.players

import com.justxdude.islandcore.utils.toLocationString
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.networkapi.commands.Command
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.networkapi.util.Utils.toDate
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EventInfoCommand : Command("wydarzenie", arrayOf("wydarzenia", "wyd")) {
    private val eventsManager = EventsManager.instance

    override fun canExecute(player: Player, args: Array<String>): Boolean = true

    override fun execute(player: Player, args: Array<String>) {
        val currentEvent = eventsManager.currentEvent
        val joined = currentEvent.playersWhoJoined.size
        val activePlayers = currentEvent.activePlayers.size
        val lowLevelMessage = listOf(
            "&c&m-".repeat(30),
            "&aInformacje na temat obecnego wydarzenia:",
            "&7",
            *currentEvent.description.toTypedArray(),
            "&7",
            "&8- &7${if(joined == 0) "Nikt jeszcze nie dołączył" else if(joined == 1) "Dołączył tylko jeden gracz" else "Łącznie dołączyło $joined graczy"}",
            "&8- &7${if(activePlayers == 0) "Nie ma żadnych aktywnych graczy w tym wydarzeniu" 
                else if(activePlayers == 1) "Tylko jeden gracz uczestniczy w wydarzeniu" 
                    else "Łączna ilość aktywnych graczy w wydarzeniu: &e$activePlayers"}",
            "&8- &7Kończy się o ${currentEvent.endsAt.toDate()}",
            "&c",
            "&cMusisz osiągnąć 10 poziom, aby dołączyć do tego wydarzenia.",
            "&c&m-".repeat(30),
        )
        val user = player.asUser() ?: return
        if(user.level < currentEvent.requiredLevel) {
            lowLevelMessage.forEach {
                player.sendColoured(it)
            }
            return
        }


        val allowedMessage = listOf(
            "&9&m-".repeat(30),
            "&aInformacje na temat obecnego wydarzenia:",
            "&7",
            *currentEvent.description.toTypedArray(),
            "&7",
            "&8- &7${if(joined == 0) "Nikt jeszcze nie dołączył" else if(joined == 1) "Dołączył tylko jeden gracz" else "Łącznie dołączyło $joined graczy"}",
            "&8- &7${if(activePlayers == 0) "Nie ma żadnych aktywnych graczy w tym wydarzeniu"
            else if(activePlayers == 1) "Tylko jeden gracz uczestniczy w wydarzeniu"
            else "Łączna ilość aktywnych graczy w wydarzeniu: &e$activePlayers"}",
            "&8- &7Kończy się o ${currentEvent.endsAt.toDate()}",
            "&c",
            "&cDołącz do tego wydarzenia poprzez portal na spawnie w koordynatach: ${currentEvent.portalLocation?.toLocationString()} (X, Y, Z).",
            "&9&m-".repeat(30),
        )

        allowedMessage.forEach {
            player.sendColoured(it)
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()

}