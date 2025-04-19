package com.justxraf.skyblockevents.commands.admin

import com.justxraf.networkapi.util.sendColoured
import com.justxraf.skyblockevents.events.Event
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.events.entities.EventEntityCuboid
import com.justxraf.skyblockevents.util.*
import io.lumine.mythic.api.MythicProvider
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

object EventAdminEntitySpawnPointSubCommand {
    /*
    /event entity spawnpoint remove
    /event entity spawnpoint create <entity_name> <level> <limit> <spawn_delay>

     */

    private fun shouldProcess(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        if(args.size < 3) {
            player.sendColoured("&cNiewystarczająca ilość argumentów! Użyj /event entity spawnpoint remove lub /event entity spawnpoint create <nazwa potwora> <poziom> <limit> <opóźnienie>.")
            return false
        }
        val world = sessionEvent.spawnLocation.world
        val location = player.location
        if(location.world != world) {
            player.sendColoured("&cMusisz znajdować się w świecie wydarzenia które obecnie modyfikujesz.")
            return false
        }
        if(args[1].lowercase() != "spawnpoint") {
            player.sendColoured("&cNiepoprawny drugi argument. Użyj /event entity spawnpoint remove lub /event entity spawnpoint create <nazwa potwora> <poziom> <limit> <opóźnienie>.")
            return false
        }
        val arguments = listOf("remove", "create")
        if(!arguments.contains(args[2])) {
            player.sendColoured("&cNiepoprawny trzeci argument. Użyj /event entity spawnpoint remove lub /event entity spawnpoint create <nazwa potwora> <poziom> <limit> <opóźnienie>.")
            return false
        }

        return true
    }
    private fun shouldProcessRemove(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        if(!sessionEvent.isInEntityCuboid(player.location)) {
            player.sendColoured("&cNie ma spawnpointa na lokacji w której się obecnie znajdujesz.")
            return false
        }
        return true
    }
    // /event entity spawnpoint create <entity_name> <level> <limit> <spawn_delay>
    private fun shouldProcessCreate(player: Player, args: Array<String>, sessionEvent: EventData): Boolean {
        if(args.size != 6) {
            player.sendColoured("&cNiepoprawna ilość argumentów! Użyj /event entity spawnpoint create <nazwa potwora> <poziom> <limit> <opóźnienie>.")
            return false
        }

        val mythicManager= MythicProvider.get().mobManager
        val mob = mythicManager.getMythicMob(args[3]).getOrNull()

        if(mob == null) {
            player.sendColoured("&cNiepoprawna nazwa potwora! Użyj /event entity spawnpoint create <nazwa potwora> <poziom> <limit> <opóźnienie>.")
            return false
        }
        try {
            val level = args[4].toInt()
        } catch (e: NumberFormatException){
            player.sendColoured("&cNiepoprawny poziom!")
            return false
        }
        try {
            val limit = args[5].toInt()
        } catch (e: NumberFormatException){
            player.sendColoured("&cNiepoprawny limit!")
            return false
        }

        val selection = player.hasWorldEditSelection()
        val selectionAnswer = selection.firstNotNullOfOrNull { it.key } ?: return false

        val cuboid = selection.firstNotNullOf { it.value }
        if(player.location.isInCuboid(cuboid)) {
            player.sendColoured("&cW tej lokacji jest już inny region! Wybierz inny region.")
            return false
        }

        return selectionAnswer == SelectionAnswer.CORRECT
    }
    private fun processRemove(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessRemove(player, args, sessionEvent)) return

        sessionEvent.removeEntityCuboidBy(player.location)

        player.sendColoured("&7Poprawnie usunięto spawnpoint w którym przebywałeś(aś).")

        currentEvent?.let {
            it.eventEntitiesHandler.removeCuboidBy(player.location)
            player.sendColoured("&7Również usunięto spawnpoint w obecnym wydarzeniu.")
        }
    }
    private fun processCreate(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcessCreate(player, args, sessionEvent)) return


        val (pos1Location, pos2Location) = player.getWorldEditSelection() ?: return

        if(sessionEvent.eventEntityCuboids.isNullOrEmpty()) sessionEvent.eventEntityCuboids = ConcurrentHashMap()

        val nextID = if(sessionEvent.eventEntityCuboids.isNullOrEmpty()) 0 else sessionEvent.eventEntityCuboids?.keys?.max() ?: 0

        val eventEntityCuboid = EventEntityCuboid(nextID + 1, args[3], Pair(pos1Location, pos2Location), args[4].toInt(), args[5].toInt())

        sessionEvent.eventEntityCuboids!![nextID + 1] = eventEntityCuboid

        player.sendColoured("&7Zapisano region dla wydarzenia #${sessionEvent.uniqueId} " +
                "z typem potwora ${args[3]} na Twojej lokacji, ID regionu to #${eventEntityCuboid.id}.")

        currentEvent?.eventEntitiesHandler?.createCuboid(eventEntityCuboid)

    }
    fun process(player: Player, args: Array<String>, sessionEvent: EventData, currentEvent: Event?) {
        if(!shouldProcess(player, args, sessionEvent)) return
        when(args[2].lowercase()) {
            "remove" -> processRemove(player, args, sessionEvent, currentEvent)
            "create" -> processCreate(player, args, sessionEvent, currentEvent)
        }
    }
}