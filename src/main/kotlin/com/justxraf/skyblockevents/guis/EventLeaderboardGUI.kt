package com.justxraf.skyblockevents.guis

import com.github.supergluelib.foundation.util.ItemBuilder
import com.github.supergluelib.guis.GUI
import com.github.supergluelib.guis.Panes
import com.github.supergluelib.guis.setBorder
import com.justxdude.skyblockapi.user.User
import com.justxraf.skyblockevents.events.EventsManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class EventLeaderboardGUI(private val player: Player, private val user: User) : GUI() {

    private val eventsManager = EventsManager.instance

    override fun generateInventory(): Inventory = createInventory(
        "Event Leaderboard", 27
    ) {
        setBorder(Panes.BLACK)

        // 11, 13
        setButton(4, information()) { }
        setButton(12, islandLeaderboardButton()) { }
        setButton(14, playerLeaderboardButton()) { }
    }
    private fun islandLeaderboardButton(): ItemStack {
        val pointsHandler = eventsManager.currentEvent.pointsHandler
        val topIslands = pointsHandler.getTopIslands()

        val lore = mutableListOf(
            "&7",
            *topIslands.map { (island, array) ->
                val color = when (array[0]) {
                    1 -> "&e"
                    2 -> "&6"
                    3 -> "&2"
                    else -> "&7"
                }
                "$color${array[0]}&7. $color${island.name} &8- &7${array[1]} Punktów."
            }.toTypedArray())
        if(!topIslands.any { it.first.uniqueId != user.islandId }) {
            if(pointsHandler.islandsLeaderboard.any { it.first == user.islandId }) {
                val pos = pointsHandler.getIslandPosition(user.islandId)
                lore += listOf(
                    "&7 ",
                    "&7Pozycja Twojej wyspy w rankingu: &b$pos&7."
                )
            }
        } else {
            lore += listOf("&7 ", "&7Żaden członek Twojej wyspy nie zdobył punktów w tym wydarzeniu!")
        }
        val item = ItemBuilder(Material.GRASS_BLOCK, "&6Tabela Wyników Wysp")
            .lore(lore)
            .build()

        return item
    }
    private fun playerLeaderboardButton(): ItemStack {
        val pointsHandler = eventsManager.currentEvent.pointsHandler
        val topPlayers = pointsHandler.getTopPlayers()

        val lore = mutableListOf(
            "&7",
            *topPlayers.map { (user, array) ->
                val color = when (array[0]) {
                    1 -> "&e"
                    2 -> "&6"
                    3 -> "&2"
                    else -> "&7"
                }
                "$color${array[0]}&7. $color${user.name} &8- &7${array[1]} Punktów."
            }.toTypedArray())

        if(topPlayers.any { it.first.uniqueId == player.uniqueId }) {
            if(pointsHandler.playersLeaderboard.any { it.first == player.uniqueId }) {
                val pos = pointsHandler.getPlayerPosition(player.uniqueId)
                lore += listOf(
                    "&7 ",
                    "&7Twoja pozycja w rankingu: &b$pos&7."
                )
            }
        } else {
            lore += listOf("&7 ", "&7Nie zdobyłeś(aś) jeszcze żadnych punktów w obecnym wydarzeniu!")
        }
        val item = ItemBuilder(Material.PLAYER_HEAD, "&6Tabela Wyników Graczy")
            .lore(lore)
            .build()

        return item
    }
    private fun information(): ItemStack = ItemBuilder(Material.PAINTING, "&aInformacja o Rankingu Wydarzenia ${eventsManager.currentEvent.name}")
        .lore(listOf("&7", "&7Punkty rankingowe możesz zdobyć poprzez:",
            "&8- &eZabijanie Potworów",
            "&8- &6Wydobywanie Surowców",
            "&8- &eWykonywanie Misji", "&7 ",
            "&7Nagrody zostaną nadane dla trzech", "&7pierwszych graczy, oraz najlepszej wyspy."
        ))
        .build()
}