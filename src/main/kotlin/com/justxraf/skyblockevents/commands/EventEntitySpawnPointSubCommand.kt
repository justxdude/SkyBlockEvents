package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getFormattedName
import com.justxraf.skyblockevents.util.isInCuboid
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

object EventEntitySpawnPointSubCommand {
    /*
    /event entity spawnpoint remove
    /event entity spawnpoint create <EntityType>

     */
    private val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") as WorldEditPlugin

    private fun shouldProcess(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        if(args.size < 3) {
            player.sendColoured("&cNiewystarczająca ilość argumentów! Użyj /event entity spawnpoint remove lub /event entity spawnpoint create <typ>.")
            return false
        }
        val world = sessionEvent.spawnLocation.world
        val location = player.location
        if(location.world != world) {
            player.sendColoured("&cMusisz znajdować się w świecie wydarzenia które obecnie modyfikujesz.")
            return false
        }
        if(args[1].lowercase() != "spawnpoint") {
            player.sendColoured("&cNiepoprawny drugi argument. Użyj /event entity spawnpoint remove lub /event entity spawnpoint create <typ>.")
            return false
        }
        val arguments = listOf("remove", "create")
        if(!arguments.contains(args[2])) {
            player.sendColoured("&cNiepoprawny trzeci argument. Użyj /event entity spawnpoint remove lub /event entity spawnpoint create <typ>.")
            return false
        }

        return true
    }
    private fun shouldProcessRemove(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        val entitySpawnpointID = sessionEvent.getSpawnPointIdAt(player.location)
        if(entitySpawnpointID == null) {
            player.sendColoured("&cNie ma spawnpointa na lokacji w której się obecnie znajdujesz.")
            return false
        }
        return true
    }
    private fun shouldProcessCreate(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        val entityType = EntityType.entries.firstOrNull { it.name.uppercase() == args[3].uppercase() }
        if(entityType == null) {
            player.sendColoured("&cTen EntityType nie istnieje! Użyj na przykład \"CREEPER\". Użyj /event setentityspawnpoint <EntityType>.")
            return false
        }
        try {
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
                    sessionEvent.spawnLocation.world,
                    selection.pos1.x.toDouble(),
                    0.0,
                    selection.pos1.z.toDouble())
                val pos2Location = Location(
                    sessionEvent.spawnLocation.world,
                    selection.pos2.x.toDouble(),
                    0.0,
                    selection.pos2.z.toDouble())

                if(player.location.isInCuboid(pos1Location, pos2Location)) {
                    player.sendColoured("&cLokacje w której obecnie przebywasz jest na terenie innego spawnpointa! " +
                            "Przejdź trochę dalej i spróbuj ponownie. Użyj /event setentityspawnpoint <EntityType>.")
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
    private fun processRemove(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessRemove(player, args, sessionEvent)) return

        val spawnPoint = sessionEvent.getSpawnPointIdAt(player.location) ?: return
        sessionEvent.spawnPointsCuboid?.remove(spawnPoint)
        sessionEvent.entityTypeForSpawnPoint?.remove(spawnPoint)

        player.sendColoured("&7Poprawnie usunięto spawnpoint w którym przebywałeś(aś).")

        currentEvent?.spawnPointsCuboid?.remove(spawnPoint)
        currentEvent?.entityTypeForSpawnPoint?.remove(spawnPoint)
    }
    private fun processCreate(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessCreate(player, args, sessionEvent)) return

        val entityType = EntityType.valueOf(args[3].uppercase())

        val session = worldEdit.getSession(player)
        val selection = session.getSelection(session.selectionWorld) as CuboidRegion
        val pos1 = selection.pos1
        val pos1Location = Location(sessionEvent.spawnLocation.world, pos1.x.toDouble(), pos1.y.toDouble() + 1, pos1.z.toDouble())
        val pos2 = selection.pos2
        val pos2Location = Location(sessionEvent.spawnLocation.world, pos2.x.toDouble(), pos2.y.toDouble() + 1, pos2.z.toDouble())

        if(sessionEvent.spawnPointsCuboid == null) sessionEvent.spawnPointsCuboid = mutableMapOf()
        if(sessionEvent.entityTypeForSpawnPoint == null) sessionEvent.entityTypeForSpawnPoint = mutableMapOf()

        val nextID = if(sessionEvent.spawnPointsCuboid.isNullOrEmpty()) 0 else sessionEvent.spawnPointsCuboid?.keys?.max() ?: 0

        sessionEvent.spawnPointsCuboid!![nextID + 1] = Pair(pos1Location, pos2Location)
        sessionEvent.entityTypeForSpawnPoint!![nextID + 1] = entityType

        player.sendColoured("&7Zapisano spawnpoint dla wydarzenia #${sessionEvent.uniqueId} " +
                "z typem potwora ${entityType.getFormattedName()} na Twojej lokacji.")

        if(currentEvent != null) {
            if(currentEvent.spawnPointsCuboid == null) currentEvent.spawnPointsCuboid = mutableMapOf()
            if(currentEvent.entityTypeForSpawnPoint == null) currentEvent.entityTypeForSpawnPoint = mutableMapOf()

            currentEvent.spawnPointsCuboid!![nextID + 1] = Pair(pos1Location, pos2Location)
            currentEvent.entityTypeForSpawnPoint!![nextID + 1] = entityType

            currentEvent.spawnPointsEntities?.put(nextID + 1, mutableMapOf())
        }
    }
    fun process(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcess(player, args, sessionEvent)) return
        when(args[2].lowercase()) {
            "remove" -> processRemove(player, args, sessionEvent, currentEvent)
            "create" -> processCreate(player, args, sessionEvent, currentEvent)
        }
    }
}