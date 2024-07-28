package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getFormattedName
import com.justxraf.skyblockevents.util.isInCuboid
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

object EventCreateSubCommand {
    private val eventsManager = EventsManager.instance
    private val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") as WorldEditPlugin
    // /event create <type>

    private fun shouldProcess(player: Player, args: Array<String>): Boolean {
        if (args.size < 2) {
            player.sendColoured("&cZła ilość argumentów! Użyj /event create <typ>.")
            return false
        }
        if(eventsManager.events.any { it.value.spawnLocation.world == player.world }) {
            player.sendColoured("&cW tym świecie już istnieje wydarzenie!")
            return false
        }
        try {
            EventType.valueOf(args[1].uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendColoured("&cNiepoprawny typ wydarzenia! Użyj /event create <typ>.")
            return false
        }

        try {
            val location = player.location
            val session = worldEdit.getSession(player)
            val selection = session.getSelection(session.selectionWorld)

            if (selection is CuboidRegion) {
                if (selection.pos1 == null) {
                    player.sendColoured("&cNie ustawiłeś pierwszej pozycji! Użyj //pos1 aby ustawić pierwszą pozycję.")
                    return false
                }
                if (selection.pos2 == null) {
                    player.sendColoured("&cNie ustawiłeś drugiej pozycji! Użyj //pos2 aby ustawić drugą pozycję.")
                    return false
                }
                val pos1Location = Location(
                    location.world,
                    selection.pos1.x.toDouble(),
                    0.0,
                    selection.pos1.z.toDouble())
                val pos2Location = Location(
                    location.world,
                    selection.pos2.x.toDouble(),
                    0.0,
                    selection.pos2.z.toDouble())

                if(player.location.isInCuboid(pos1Location, pos2Location)) {
                    player.sendColoured("&cLokacje w której obecnie przebywasz jest na terenie innego spawnpointa! " +
                            "Przejdź trochę dalej i spróbuj ponownie. Użyj /event create <typ>.")
                    return false
                }
                return true
            } else {
                player.sendColoured("&cNie wyznaczyłeś poprawnie granic! Użyj //pos1 i //pos2 aby wyznaczyć granice.")
                return false
            }
        } catch (e: Exception) {
            player.sendColoured("&cNie wyznaczyłeś poprawnie granic! Użyj //pos1 i //pos2 aby wyznaczyć granice.")
            return false
        }
    }
    fun process(player: Player, args: Array<String>) {
        if(!shouldProcess(player, args)) return
        val type = EventType.valueOf(args[1].uppercase())

        val location = player.location
        val lastIP = eventsManager.events.keys.maxOrNull() ?: 1

        val session = worldEdit.getSession(player)
        val selection = session.getSelection(session.selectionWorld) as CuboidRegion
        val pos1 = selection.pos1
        val pos1Location = Location(location.world, pos1.x.toDouble(), pos1.y.toDouble() + 1, pos1.z.toDouble())
        val pos2 = selection.pos2
        val pos2Location = Location(location.world, pos2.x.toDouble(), pos2.y.toDouble() + 1, pos2.z.toDouble())

        val event = when(type) {
            EventType.NETHER -> EventData(
                type.getFormattedName(),
                lastIP + 1,
                type,
                0,
                0,
                mutableListOf(""),
                location,
                10,
                Pair(pos1Location, pos2Location)
            )
            else -> EventData(type.getFormattedName(),
                lastIP + 1,
                type,
                0,
                0,
                mutableListOf(""),
                location,
                10,
                Pair(pos1Location, pos2Location)
            )

        }
        eventsManager.events[event.uniqueId] = event
        eventsManager.saveEvent(event)

        eventsManager.editSession[player.uniqueId] = event
        player.sendColoured("&aPomyślnie utworzono wydarzenie ${event.name} #${event.uniqueId}.")
    }
}