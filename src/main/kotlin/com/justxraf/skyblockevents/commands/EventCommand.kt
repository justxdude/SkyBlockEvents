package com.justxraf.skyblockevents.commands

import com.github.supergluelib.foundation.util.ItemBuilder
import com.justxdude.networkapi.commands.Command
import com.justxdude.networkapi.util.Utils.sendColoured
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserExtensions.sendColoured
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.util.getFormattedName
import com.justxraf.skyblockevents.util.isInPortal
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import java.util.*

class EventCommand : Command("event", arrayOf("e"), "hyperiol.skyblockevents.event") {

    private val eventsManager = EventsManager.instance
    private val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") as WorldEditPlugin
    private val editSession = mutableMapOf<UUID, Int>()
    private val sessionCommands = listOf("SETSPAWN",
        "SETPORTAL",
        "SETQUESTNPC",
        "ADDQUEST",
        "ADDTODESCRIPTION",
        "CLEARDESCRIPTION",
        "SETENTITYSPAWNPOINT",
        "SETEVENTPORTAL",
        "GETREGENERATIVEBLOCK",
        "GETREGENERATIVEBLOCKREMOVER",
        "REMOVEENTITYSPAWNPOINT"
    )
    /*

    TODO:
     /event seteventportal - Sets the location of the portal in the event world to come back to spawn,
     /event getregenerativeblock <material> - Gives a block to the player which can be placed and is added to the list of regenerative blocks
     /event getregenerativeblockremover - Gives a tool which can remove regenerative blocks
     /event removeentityspawnpoint - Removes the spawnpoint for entity in the session event

    Structure:

    /event
    /event create <type>
    /event edit <id> // opens the editing session for a given event
    /event destroysession

    /event setspawn
    /event setportal
    /event setquestnpc // for location only
    /event addquest
    /event addtodescription <string...> // adds to the existing description
    /event cleardescription // clears the description

     */

    override fun canExecute(player: Player, args: Array<String>): Boolean {
        if(args.isEmpty()) {
            player.sendColoured("&cNiepoprawne użycie! Dostępne komendy:")
            if(editSession[player.uniqueId] == null) {
                player.sendColoured("&c/event create <typ> &8- &7Tworzy wydarzenie o wybranym typie z respawnem w lokacji w której przebywasz.")
                player.sendColoured("&c/event start <id> &8- &7Startuje wydarzenie o wybranym ID i kończy obecne wydarzenie.")
                player.sendColoured("&c/event edit <id> &8- &7Otwiera sesję edytowania wydarzenia o wybranym ID.")
            } else {
                player.sendColoured("&c/event create <typ> &8- &7Tworzy wydarzenie o wybranym typie z respawnem w lokacji w której przebywasz.")
                player.sendColoured("&c/event start <id> &8- &7Startuje wydarzenie o wybranym ID i kończy obecne wydarzenie.")
                player.sendColoured("&c/event edit <id> &8- &7Otwiera sesję edytowania wydarzenia o wybranym ID.")
                player.sendColoured("&c/event setspawn")
                player.sendColoured("&c/event setportal &8- &7Ustawia portal przez który gracze mogą się przeteleportować do wydarzenia.")
                player.sendColoured("&c/event setquestnpc &8- &7Ustawia lokację w której NPC z zadaniami się pojawi.")
                player.sendColoured("&c/event addquest <id> &8- &7Dodaje zadanie o wybranym ID do NPC z zadaniami.")
                player.sendColoured("&c/event addtodescription <tekst> &8- &7Dodaje linię do opisu wydarzenia.")
                player.sendColoured("&c/event cleardescription &8- &7Czyści cały opis wydarzenia.")
                val event = eventsManager.events[editSession[player.uniqueId]]
                if(event != null && event.eventType == EventType.NETHER) {
                    player.sendColoured("&c/event setentityspawnpoint <EntityType> &8- &7Ustawia spawnpoint dla wybranego typu potwora.")
                    player.sendColoured("&c/event seteventportal &8- &7Ustawia portal który jest w świecie wydarzenia.")
                    player.sendColoured("&c/event getregenerativeblock <materiał> &8- &7Daje Ci regenerujący się blok.")
                    player.sendColoured("&c/event getregenerativeblockremover &8- &7Daje Ci narzędzie do usuwania regenerujących się bloków.")
                    player.sendColoured("&c/event removeentityspawnpoint &8- &7Usuwa spawnpoint w lokacji w której obecnie przebywasz.")
                }
            }
            return false
        }
        if(sessionCommands.contains(args[0].uppercase()) && !editSession.contains(player.uniqueId)) {
            player.sendColoured("&cMusisz najpierw otworzyć sesję edytowania wydarzenia, " +
                    "aby użyć /event ${args[0]}! Użyj /event edit <id> lub /event create <typ>.")
            return false
        } else if(sessionCommands.contains(args[0]) && editSession.contains(player.uniqueId)) {
            val oneArgCommands = listOf("SETSPAWN",
                "SETPORTAL",
                "CLEARDESCRIPTION",
                "SETQUESTNPC",
                "SETEVENTPORTAL",
                "GETREGENERATIVEBLOCKREMOVER")
            if(args[0] == "REMOVEENTITYSPAWNPOINT") {
                val event = eventsManager.events[editSession[player.uniqueId]] ?: return false
                if(event.spawnLocation.world != player.world) {
                    player.sendColoured("&cMusisz znajować się w świecie wydarzenia!")
                    return false
                }
                val entitySpawnpointID = event.getSpawnPointIdAt(player.location)
                if(entitySpawnpointID == null) {
                    player.sendColoured("&cNie ma spawnpointa na lokacji w której się obecnie znajdujesz.")
                    return false
                }
                return true
            }
            if(oneArgCommands.contains(args[0].uppercase())) return true else {
                if(args.size < 2) {
                    player.sendColoured("&cMusisz użyć przynajmniej dwóch argumentów! /event ${args[0]}.")
                    return false
                }
                if(args[0].uppercase() == "GETREGENERATIVEBLOCK") { // /event getregenerativeblock <materia>
                    val material = Material.entries.firstOrNull { it.name == args[1].uppercase() }
                    if(material == null) {
                        player.sendColoured("&cUżyj poprawnego materiału w drugim argumencie! Użyj /event getregenerativeblock <materiał>.")
                        return false
                    }
                    return true
                }
                if(args[0].uppercase() == "ADDQUEST") {
                    try {
                        val id = args[1].toInt()
                    }catch (e: NumberFormatException) {
                        player.sendColoured("&cUżyj numerów jako ID zadania!")
                        return false
                    }
                    return true
                }
                if(args[0].uppercase() == "SETENTITYSPAWNPOINT") {
                    if(args.size != 2) {
                        player.sendColoured("&cNiepoprawna ilość argumentów! Użyj /event setentityspawnpoint <EntityType>")
                        return false
                    }
                    val entityType = EntityType.entries.firstOrNull { it.name.uppercase() == args[1].uppercase() }
                    if(entityType == null) {
                        player.sendColoured("&cTen EntityType nie istnieje! Użyj na przykład \"CREEPER\". Użyj /event setentityspawnpoint <EntityType>.")
                        return false
                    }
                    try {
                        val event = eventsManager.events[editSession[player.uniqueId]] ?: return false
                        if(event.spawnLocation.world != player.world) {
                            player.sendColoured("&cMusisz znajować się w świecie wydarzenia!")
                            return false
                        }
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
                                event.spawnLocation.world,
                                selection.pos1.x.toDouble(),
                                0.0,
                                selection.pos1.z.toDouble())
                            val pos2Location = Location(
                                event.spawnLocation.world,
                                selection.pos2.x.toDouble(),
                                0.0,
                                selection.pos2.z.toDouble())

                            if(isInPortal(player.location, pos1Location, pos2Location)) {
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
                return true
            }
        }
        if(args[0].uppercase() == "START") {
            if(args.size < 2) {
                player.sendColoured("&cZła ilość argumentów! Użyj /event start <id>.")
                return false
            }
            try {
                val event = eventsManager.events[args[1].toInt()]
                if(event == null) {
                    player.sendColoured("&cTe wydarzenie nie istnieje! Użyj /event start <id>.")
                    return false
                }
                if(eventsManager.currentEvent.uniqueId == args[1].toInt()) {
                    player.sendColoured("&cTe wydarzenie już wystartowało! Użyj /event start <id>.")
                    return false
                }
                return true

            } catch (e: NumberFormatException) {
                player.sendColoured("&cMożesz użyć tylko liczb w drugim argumencie! Użyj /event start <id>.")
                return false
            }
        }
        if(args[0].uppercase() == "CREATE") {
            if(args.size < 2) {
                player.sendColoured("&cZła ilość argumentów! Użyj /event create <typ>.")
                return false
            }
            try {
                EventType.valueOf(args[1].uppercase())
            } catch (e: IllegalArgumentException) {
                player.sendColoured("&cNiepoprawny typ wydarzenia! Użyj /event create <typ>.")
                return false
            }
            return true
        }
        if(args[0].uppercase() == "EDIT") {
            if(args.size < 2) {
                player.sendColoured("&cZła ilość argumentów! Użyj /event edit <id>.")
                return false
            }
            try {
                val id = args[1].toInt()
                if(eventsManager.currentEvent.uniqueId == id) {
                    player.sendColoured("&cNie możesz edytować tego wydarzenia, ponieważ jest ono obecnie aktywne!")
                    return false
                }

                val event = eventsManager.events[id]
                if(event == null) {
                    player.sendColoured("&cWydarzenie o tym ID nie istnieje! Użyj /event edit <id>.")
                    return false
                }

                return true
            } catch (e: NumberFormatException) {
                player.sendColoured("&cUżyj tylko liczb jako drugi argument! Użyj /event edit <id>.")
                return false
            }
        }
        if(args[0].uppercase() == "DESTROYSESSION") {
            if(!editSession.contains(player.uniqueId)) {
                player.sendColoured("&cNie jesteś obecnie w żadnej sesji edytowania! Użyj /event edit <id> lub /event create <typ>.")
                return false
            }
            return true
        }
        return true
    }

    override fun execute(player: Player, args: Array<String>) {

        if(args[0].uppercase() == "START") processStart(player, args)
        if(args[0].uppercase() == "CREATE") processCreate(player, args)
        if(args[0].uppercase() == "EDIT") processEdit(player, args)

        if(args[0].uppercase() == "DESTROYSESSION") processDestroySession(player)
        if(sessionCommands.contains(args[0].uppercase())) processSessionCommands(player, args)

    }
    private fun processStart(player: Player, args: Array<String>) {
        eventsManager.currentEvent.end()

        val event = eventsManager.events[args[1].toInt()]?.fromData() ?: return
        eventsManager.currentEvent = event
        event.start()
    }
    private fun processSessionCommands(player: Player, args: Array<String>) {
        val event = eventsManager.events[editSession[player.uniqueId]] ?: return

        when(args[0].uppercase()) {
            "SETSPAWN" -> {
                event.spawnLocation = player.location
                player.sendColoured("&aZmieniono spawn dla wydarzenia ${event.name} na ${player.location.x} ${player.location.y} ${player.location.z} (XYZ)")
            }

            "SETPORTAL" -> {
                event.portalLocation = player.location
                player.sendColoured("&aZmieniono lokację portalu na ${event.name} na ${player.location.x} ${player.location.y + 1} ${player.location.z} (XYZ)")
            }

            "SETQUESTNPC" -> {
                event.questNPCLocation = player.location
                player.sendColoured("&aZmieniono lokalizację NPC z zadaniami dla wydarzenia ${event.name} na ${player.location.x} ${player.location.y} ${player.location.z} (XYZ)")
            }

            "ADDQUEST" -> {
                event.quests?.plusAssign(args[1].toInt())
                player.sendColoured("&aDodano zadanie o ID ${args[1]} dla wydarzenia ${event.name}")
            }

            "ADDTODESCRIPTION" -> {
                val message = args.slice(1 until args.size).joinToString(" ")
                event.description += message

                player.sendColoured("&aDodano nową linię do opisu dla wydarzenia ${event.name} o treści: $message")
            }

            "CLEARDESCRIPTION" -> {
                event.description.clear()
                player.sendColoured("&aWyczyszczono opis dla wydarzenia ${event.name}. Dodaj nowy opis przy uzyciu komendy /event addtodescription <treść>")
            }

            "SETENTITYSPAWNPOINT" -> {
                processSetEntitySpawnpoint(player, args)
            }
            "SETEVENTPORTAL" -> {
                if(player.location.world != event.spawnLocation.world) {
                    player.sendColoured("&cPortal wydarzenia powinien być w tym samym świecie, w którym jest wydarzenie!")
                    return
                }
                event.eventPortalLocation = player.location
                player.sendColoured("&aUstawiono portal wydarzenia w Twojej lokacji.")
            }
            "GETREGENERATIVEBLOCK" -> {
                val material = Material.valueOf(args[1])
                val item = ItemBuilder(material, "&cRegenerujący blok").lore(listOf("&7Połóż ten blok w świecie",
                    "&7i będzie on regenerowany",
                    "&7za każdym razem gdy gracz",
                    "&7go zniszczy!"))
                    .hideEnchants(true)
                    .addEnchant(Enchantment.DURABILITY, 1)
                    .locname("regenerative_block")
                    .build()

                player.inventory.setItem(EquipmentSlot.HAND, item)

                player.sendColoured("&aOtrzymałeś przedmiot do stawiania bloków regenerujących (użyj tylko do wydarzeń).")
            }
            "GETREGENERATIVEBLOCKREMOVER" -> {
                val item = ItemBuilder(Material.WOODEN_PICKAXE, "&cUsuwacz do regenerujących bloków")
                    .locname("regenerative_block_remover")
                    .lore(listOf("&7Kliknij na regenerujący blok", "&7Aby go usunąć."))
                    .hideEnchants(true)
                    .addEnchant(Enchantment.DURABILITY, 1)
                    .build()
                player.inventory.setItem(EquipmentSlot.HAND, item)

                player.sendColoured("&aOtrzymałeś przedmiot do usuwania bloków regenerujących (użyj tylko do wydarzeń).")
            }
            "REMOVEENTITYSPAWNPOINT" -> {
                val spawnPoint = event.getSpawnPointIdAt(player.location) ?: return
                event.spawnPointsCuboid?.remove(spawnPoint)
                event.entityTypeForSpawnPoint?.remove(spawnPoint)

                player.sendColoured("&aUsunięto spawnpoint w którym przebywałeś poprawnie.")
            }
            else -> return
        }
    }
    /*
     /event removeentityspawnpoint - Removes the spawnpoint for entity in the session event
     */
    private fun processEdit(player: Player, args: Array<String>) {
        val id = args[1].toInt()
        editSession[player.uniqueId] = id
        player.sendColoured("&aPomyślnie dodano Cię do sesji edytowania wydarzenia o id ${editSession[player.uniqueId]}.")
    }
    private fun processDestroySession(player: Player) {
        player.sendColoured("&aPomyślnie usunięto z sesji edytowania wydarzenia o id ${editSession[player.uniqueId]}.")
        editSession.remove(player.uniqueId)
    }
    private fun processCreate(player: Player, args: Array<String>) {
        val type = EventType.valueOf(args[1].uppercase())
        val location = player.location
        val lastIP = eventsManager.events.keys.maxOrNull() ?: 1
        val event = when(type) {
            EventType.NETHER -> EventData(
                type.getFormattedName(),
                lastIP + 1,
                type,
                0,
                0,
                location.world!!.name,
                mutableListOf(""),
                location)
            else -> {
                EventData(
                    type.getFormattedName(),
                    lastIP + 1,
                    type,
                    0,
                    0,
                    location.world!!.name,
                    mutableListOf(""),
                    location)
            }
        }
        eventsManager.events[event.uniqueId] = event
        eventsManager.saveEvent(event)

        editSession[player.uniqueId] = event.uniqueId
        player.sendColoured("&aPomyślnie utworzono wydarzenie ${event.name} (id ${event.uniqueId}).")
    }
    private fun processSetEntitySpawnpoint(player: Player, args: Array<String>) {
        val entityType = EntityType.valueOf(args[1].uppercase())
        val event = eventsManager.events[editSession[player.uniqueId]] ?: return

        val session = worldEdit.getSession(player)
        val selection = session.getSelection(session.selectionWorld) as CuboidRegion
        val pos1 = selection.pos1
        val pos1Location = Location(event.spawnLocation.world, pos1.x.toDouble(), 0.0, pos1.z.toDouble())
        val pos2 = selection.pos1
        val pos2Location = Location(event.spawnLocation.world, pos2.x.toDouble(), 0.0, pos2.z.toDouble())

        if(event.spawnPointsCuboid == null) event.spawnPointsCuboid = mutableMapOf()
        if(event.entityTypeForSpawnPoint == null) event.entityTypeForSpawnPoint = mutableMapOf()

        val nextID = event.spawnPointsCuboid!!.keys.max() + 1
        event.spawnPointsCuboid!![nextID] = Pair(pos1Location, pos2Location)
        event.entityTypeForSpawnPoint!![nextID] = entityType

        player.sendColoured("&7Zapisano spawnpoint dla ${entityType.getFormattedName()} na Twojej lokacji.")
    }
    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val user = sender.asUser() ?: return emptyList()
        val session = editSession[user.uniqueId]
        if(session == null && args.size == 1) return listOf("create", "edit", "destroysession")

        val event = eventsManager.events[session]
        val sessionCommands = mutableListOf("create", "edit", "destroysession", "setspawn", "setportal",
            "setquestnpc", "addquest", "addtodescription", "cleardescription")
        if(event == null) return listOf("create", "edit", "start").sorted()
        if(args.size == 1) return if(event.eventType == EventType.NETHER) sessionCommands
            .plus(listOf("setentityspawnpoint", "seteventportal", "getregenerativeblock", "getregenerativeblockremover", "removeentityspawnpoint"))
        else sessionCommands

        val events = EventsManager.instance.events.map { it.key.toString() }
        if(args.size == 2) return if(args[0] == "create") EventType.entries.map { it.getFormattedName() }
        else if(args[0] == "edit") events else if(args[0] == "getregenerativeblock")
            return Material.entries.map { it.name.lowercase() }.sorted()
        else if(args[0] == "setentityspawnpoint") EntityType.entries.map { it.name.lowercase() }
        else return emptyList()

        return emptyList()
    }
}