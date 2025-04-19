package com.justxraf.skyblockevents.events.portals

import com.justxdude.skyblockapi.user.User
import com.justxraf.networkapi.util.Utils.toDate
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventType
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.translations.SkyBlockEventsResourcesManager
import com.justxraf.skyblockevents.util.localeEventsTranslation
import com.justxraf.skyblockevents.util.removeViewer
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import de.oliver.fancyholograms.api.HologramManager
import de.oliver.fancyholograms.api.data.TextHologramData
import de.oliver.fancyholograms.api.data.property.Visibility
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.FileInputStream
import kotlin.io.use
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min
import kotlin.use

class EventPortal(
    val portalType: EventPortalType,
    private val direction: String,
    val centre: Location,
    private val eventType: EventType,
    private var cuboid: Pair<Location, Location>? = null
) {
    @Transient private var task: BukkitTask? = null
    @Transient
    var event = EventsManager.instance.currentEvent
    @Transient
    lateinit var hologramLocation: Location
    @Transient
    lateinit var schematicPath: String
    fun setup(): EventPortal {
        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable {
            val centreClone = centre.clone()
            hologramLocation = when (direction) {
                "East" -> Location(centreClone.world, centreClone.x - 0.5, centreClone.y + 4, centreClone.z + .5)
                "West" -> Location(centreClone.world, centreClone.x + 1.3, centreClone.y + 4, centreClone.z + .5)
                "South" -> Location(centreClone.world, centreClone.x + .5, centreClone.y + 4, centreClone.z - .5)
                "North" -> Location(centreClone.world, centreClone.x + .5, centreClone.y + 4, centreClone.z + 1.2)
                else -> Location(centreClone.world, 0.0, 0.0, 0.0)
            }
            schematicPath = direction.lowercase() +
                    portalType.name.lowercase().replaceFirstChar { it.uppercase() } +
                    eventType.name.lowercase().replaceFirstChar { it.uppercase() }

            event = EventsManager.instance.currentEvent

            runTask()
            place()

        }, 5)
        return this
    }
    fun end(removalReason: PortalRemovalReason) {
        stopTask()
        remove(removalReason)
    }
    fun runTask() {
        task = object : BukkitRunnable() {
            override fun run() {
                reloadHologram()
            }
        }.runTaskTimer(SkyBlockEvents.instance, 0, 200)
    }
    fun stopTask() {
        task?.cancel()
    }
    private fun normalHologramLore(locale: String, hasRequiredLevel: Boolean) = listOf(
        when (event.eventType) {
            EventType.NETHER -> "nether".localeEventsTranslation(locale)
            EventType.FISHING -> "fishing".localeEventsTranslation(locale)
        },
        "active.players".localeEventsTranslation(locale, event.eventUserHandler.users.size.toString()),
        if (event.eventUserHandler.users.isEmpty()) "nobody.joined".localeEventsTranslation(locale)
        else if (event.eventUserHandler.users.size == 1) "joined.one.player".localeEventsTranslation(locale)
        else "joined.in.total".localeEventsTranslation(locale, event.eventUserHandler.users.size.toString()),
        "ends.in".localeEventsTranslation(locale, event.endsAt.toDate()),
        if(hasRequiredLevel) "enter.portal.to.join".localeEventsTranslation(locale)
        else "cannot.enter.portal".localeEventsTranslation(locale, event.requiredLevel.toString())
    )
    private fun eventHologramLore(locale: String) = listOf(
    "teleport.to".localeEventsTranslation(locale),
    "spawn".localeEventsTranslation(locale)
    )
    private fun reloadHologram() {
        try {
            val manager = FancyHologramsPlugin.get().hologramManager

            SkyBlockEventsResourcesManager.instance.languages.forEach { locale ->
                val hologram = manager.getHologram("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale").getOrNull()
                if (hologram == null) {
                    placeHologram()
                    return@forEach
                }
                if(portalType == EventPortalType.NORMAL) {
                    val requirementHologram =
                        manager.getHologram("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale").getOrNull()
                    if(requirementHologram == null) {
                        placeHologram()
                        return@forEach
                    }
                    val requirementHologramData = requirementHologram.data as? TextHologramData ?: return@forEach
                    requirementHologramData.text = normalHologramLore(locale, false)
                }

                val hologramData = hologram.data as? TextHologramData ?: return@forEach
                hologramData.text = if(portalType == EventPortalType.NORMAL)
                    normalHologramLore(locale, true) else eventHologramLore(locale)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun placeHologram() {
        try {
            val hologramManager = FancyHologramsPlugin.get().hologramManager

            SkyBlockEventsResourcesManager.instance.languages.forEach { locale ->
                val hologram = hologramManager.getHologram("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale").getOrNull()
                if(hologram != null) {
                    return@forEach
                }

                val hologramData = TextHologramData(
                    "${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale",
                    hologramLocation)

                hologramData.text = if(portalType == EventPortalType.EVENT) eventHologramLore(locale)
                    else normalHologramLore(locale, true)
                hologramData.billboard = Display.Billboard.CENTER
                hologramData.visibilityDistance = 20
                hologramData.visibility = Visibility.MANUAL

                if(portalType == EventPortalType.NORMAL) {
                    val hologram = hologramManager.getHologram("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale").getOrNull()
                    if(hologram != null) {
                        return@forEach
                    }

                    val requirementHologramData = TextHologramData(
                        "${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale",
                        hologramLocation
                    )


                    requirementHologramData.text = normalHologramLore(locale, false)

                    requirementHologramData.billboard = Display.Billboard.CENTER
                    requirementHologramData.visibilityDistance = 20
                    requirementHologramData.visibility = Visibility.MANUAL

                    hologramManager.addHologram(hologramManager.create(requirementHologramData))
                }
                hologramManager.addHologram(hologramManager.create(hologramData))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun removeHologram() {
        try {
            val hologramManager = FancyHologramsPlugin.get().hologramManager

            SkyBlockEventsResourcesManager.instance.languages.forEach { locale ->
                val hologram = hologramManager.getHologram("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale").getOrNull()
                if(hologram != null) hologramManager.removeHologram(hologram)

                if(portalType == EventPortalType.NORMAL) {
                    val requirementHologram = hologramManager.getHologram(
                        "${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale")
                        .getOrNull()
                    if(hologram != null) hologramManager.removeHologram(requirementHologram)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun remove(removalReason: PortalRemovalReason) {
        if(removalReason == PortalRemovalReason.RESTART) return
        removeHologram()

        if(cuboid == null) return

        val (pos1, pos2) = cuboid ?: throw NullPointerException("Cuboid in the Event Portal is null!")
        val world = BukkitAdapter.adapt(pos1.world)
        val region = CuboidRegion(world, BlockVector3.at(pos1.x + 0.5, pos1.y, pos1.z + 0.5), BlockVector3.at(pos2.x + 0.5, pos2.y, pos2.z - 1.5))

        WorldEdit.getInstance().newEditSessionBuilder().world(world).build().use {
            val airBlock = BukkitAdapter.adapt(org.bukkit.Material.AIR.createBlockData())
            it.setBlocks(region.faces, airBlock)
        }
    }
    fun place() {
        try {
            cuboid = pasteSchematic()

            removeHologram()
            placeHologram()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pasteSchematic(): Pair<Location, Location>? {
        val clipboard = loadSchematic()

        val region = clipboard?.region ?: return null
        // Calculate Cuboid positions for the portal
        val pos1 = getPosition(region.minimumPoint)
        val pos2 = getPosition(region.maximumPoint)

        val worldEdit = WorldEdit.getInstance()
        val session = worldEdit.newEditSessionBuilder().world(BukkitWorld(centre.world)).build()
        try {
            val placementPosition = BlockVector3.at(centre.x, centre.y, centre.z)

            // Paste the schematic
            val operation = ClipboardHolder(clipboard)
                .createPaste(session)
                .to(placementPosition)
                .ignoreAirBlocks(false)
                .build()

            Operations.complete(operation)

            return Pair(pos1, pos2)
        } finally {
            session.close()
        }
    }

    private fun loadSchematic(): Clipboard? {
        val schematicsFolder = File(SkyBlockEvents.instance.dataFolder, "schematics")
        val schematicFile = File(schematicsFolder, "$schematicPath.schem")

        if (!schematicFile.exists()) {
            SkyBlockEvents.instance.logger.info("Schematic $schematicPath is null!")
            return null
        }

        val format = ClipboardFormats.findByFile(schematicFile)
        format?.let {
            val clipboardReader = it.getReader(FileInputStream(schematicFile))
            clipboardReader.use { reader ->
                return reader.read()
            }
        }

        return null
    }
    private fun getPosition(vector3: BlockVector3): Location {
        return Location(centre.world,
            vector3.x.plus(centre.x),
            vector3.y.plus(centre.y),
            vector3.z.plus(centre.z))
    }
    fun isIn(loc: Location?): Boolean {
        if (loc == null || loc.world != centre.world) return false

        val x = loc.x
        val z = loc.z
        val y = loc.y

        val (pos1, pos2) = cuboid ?: throw NullPointerException("Cuboid in Current Event portal is null!")

        val tolerance = 0.5
        val (x1, y1, z1) = arrayOf(pos1.x, pos1.y, pos1.z)
        val (x2, y2, z2) = arrayOf(pos2.x, pos2.y, pos2.z)

        return x in (min(x1, x2) - tolerance)..(max(x1, x2) + tolerance) &&
                y in (min(y1, y2) - tolerance)..(max(y1, y2) + tolerance) &&
                z in (min(z1, z2) - tolerance)..(max(z1, z2) + tolerance)
    }
    fun showHologram(player: Player, user: User, hologramManager: HologramManager, skyBlockEventsResourcesManager: SkyBlockEventsResourcesManager) {
        val locale = if(!skyBlockEventsResourcesManager.languages.contains(player.locale)) "en_us" else player.locale

        val hologramName = if(user.level > event.requiredLevel) "${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale"
                            else "${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale"

        val hologram = hologramManager.getHologram(hologramName).getOrNull() ?: return

        if(!hologram.isWithinVisibilityDistance(player)) return
        if(!hologram.isViewer(player)) {
            Visibility.ManualVisibility.addDistantViewer(hologram, player.uniqueId)
            hologram.forceShowHologram(player)
        }

    }
    fun removeDistantViewerFromHologram(player: Player, user: User, skyBlockEventsResourcesManager: SkyBlockEventsResourcesManager, hologramManager: HologramManager) {
        val hologramNames = mutableListOf<String>()
        val languages = skyBlockEventsResourcesManager.languages

        val playerLocale = if(languages.contains(player.locale)) player.locale else "en_us"

        languages.forEach { locale ->
            if(locale == playerLocale) { // Player's level might change,
                // so it's necessary to remove a hologram
                // to avoid duplicates.
                if(user.level < event.requiredLevel) {
                    hologramNames.add("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale")
                } else{
                    hologramNames.add("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale")
                }
                return@forEach
            }
            hologramNames.add("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_$locale")
            hologramNames.add("${event.uniqueId}_${portalType.name.lowercase()}_portal_hologram_requirement_$locale")
        }
        // If it doesn't contain player's locale and player is a viewer - remove
        val holograms = hologramNames.map { hologramManager.getHologram(it).getOrNull() }

        holograms.removeViewer(player)
    }
}