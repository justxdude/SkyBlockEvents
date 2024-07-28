package com.justxraf.skyblockevents.commands

import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.*
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
        val selection = player.hasWorldEditSelection()
        val selectionAnswer = selection.firstNotNullOfOrNull { it.key } ?: return false

        val (pos1, pos2) = selection.firstNotNullOf { it.value }
        if(player.location.isInCuboid(pos1, pos2)) {
            player.sendColoured("&cW tej lokacji jest już inny region! Wybierz inny region.")
            return false
        }

        return selectionAnswer == SelectionAnswer.CORRECT
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

        val (pos1Location, pos2Location) = player.getWorldEditSelection() ?: return

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