package com.justxraf.skyblockevents.util

import com.github.supergluelib.foundation.util.ItemBuilder
import com.justxdude.skyblockapi.utils.Util.translate
import com.justxraf.networkapi.util.sendColoured
import com.justxraf.networkapi.util.toColorComponent
import com.justxraf.skyblockevents.translations.SkyBlockEventsResourcesManager
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.regions.CuboidRegion
import de.oliver.fancyholograms.api.data.property.Visibility
import de.oliver.fancyholograms.api.hologram.Hologram
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.max
import kotlin.math.min
private val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") as WorldEditPlugin

fun Player.pushIfClose(targetLocation: Location, threshold: Double, pushStrength: Double) {
    val playerLocation = location

    val distance = playerLocation.distance(targetLocation)

    if (distance > 0 && distance <= threshold) {
        val direction = playerLocation.toVector().subtract(targetLocation.toVector()).normalize()

        val velocity = direction.multiply(pushStrength)
        this.velocity = velocity
    }
}

fun Long.toTimeAgo(): String {
    val now = Instant.now()
    val past = Instant.ofEpochMilli(this)
    val duration = Duration.between(past, now)

    return when {
        duration.toDays() > 0 -> "${duration.toDays()} days"
        duration.toHours() > 0 -> "${duration.toHours()} hours"
        duration.toMinutes() > 0 -> "${duration.toMinutes()} minutes"
        else -> "${duration.toSeconds()} seconds"
    }
}
fun Enum<*>.getFormattedName() = name
    .split("_")
    .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

fun getEndOfDayMillis(timestamp: Long): Long {
    val zone = ZoneId.of("Europe/Berlin")
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone)

    val endOfDay = zdt
        .withHour(23)
        .withMinute(59)
        .withSecond(59)
        .withNano(999999999)

    return endOfDay.toInstant().toEpochMilli()
}

fun MutableMap<UUID, Long>.shouldSendMessage(uniqueId: UUID): Boolean {
    if(this[uniqueId] == null) {
        this[uniqueId] = System.currentTimeMillis()
        return true
    }
    if(System.currentTimeMillis() - this[uniqueId]!! > 6000) {
        this[uniqueId] = System.currentTimeMillis()
        return true
    }
    return false
}

fun Location.isInCuboid(pair: Pair<Location, Location>): Boolean {
    val tolerance = 0.5

    val (pos1, pos2) = pair

    val (x1, y1, z1) = arrayOf(pos1.x, pos1.y, pos1.z - 1.0)
    val (x2, y2, z2) = arrayOf(pos2.x, pos2.y, pos2.z - 1.0)

    return x in (min(x1, x2 - tolerance))..(max(x1, x2 + tolerance)) &&
            y in (min(y1, y2 - tolerance))..(max(y1, y2 + tolerance)) &&
            z in (min(z1, z2 - tolerance))..(max(z1, z2 + tolerance))
}

fun Player.hasWorldEditSelection(sendMessage: Boolean = true): MutableMap<SelectionAnswer, Pair<Location, Location>?> {
    try {
        val session = worldEdit.getSession(player)
        val selection = session.getSelection(session.selectionWorld)

        if (selection is CuboidRegion) {
            if (selection.pos1 == null) {
                sendColoured("&cNie ustawiłes/as poprawnie pos1 w WorldEdit. Użyj /pos1 aby ustawić pierwszą pozycję dla regionu.")
                return mutableMapOf(SelectionAnswer.NULL_POS1 to null)
            }
            if (selection.pos2 == null) {
                sendColoured("&cNie ustawiles/as poprawnie pos2 w WorldEdit. Użyj /pos2 aby ustawić drugą pozycję dla regionu.")
                return mutableMapOf(SelectionAnswer.NULL_POS2 to null)
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

            return mutableMapOf(SelectionAnswer.CORRECT to Pair(pos1Location, pos2Location))
        } else {
            sendColoured("&cWystąpił błąd podczas tworzenia regionu! Ustaw region poprzez użycie /pos1 i /pos2 lub przy użyciu drewnianej siekierki //wand.")
            return mutableMapOf(SelectionAnswer.NULL to null)
        }
    } catch (e: Exception) {
        sendColoured("&cWystąpił błąd podczas tworzenia regionu! Ustaw region poprzez użycie /pos1 i /pos2 lub przy użyciu drewnianej siekierki //wand.")
        return mutableMapOf(SelectionAnswer.NULL to null)
    }
}
fun Player.getWorldEditSelection(): Pair<Location, Location>? {
    val session = worldEdit.getSession(player)
    val selection = session.getSelection(session.selectionWorld) as CuboidRegion
    val pos1 = selection.pos1
    val pos1Location = Location(world, pos1.x.toDouble(), pos1.y.toDouble() + 1, pos1.z.toDouble())
    val pos2 = selection.pos2
    val pos2Location = Location(world, pos2.x.toDouble(), pos2.y.toDouble() + 1, pos2.z.toDouble())

    return Pair(pos1Location, pos2Location)
}

fun Player.getLookingDirection(): String {
    val yaw: Float = location.yaw
    return if (yaw > 135 || yaw < -135) {
        "North"
    } else if (yaw < -45) {
        "East"
    } else if (yaw > 45) {
        "West"
    } else {
        "South"
    }
}

enum class SelectionAnswer {
    NULL_POS1, NULL_POS2, NULL, CORRECT
}
val skyBlockEventsResourcesManager = SkyBlockEventsResourcesManager.instance

fun String.eventsTranslation(player: Player, vararg arguments: String) =
    skyBlockEventsResourcesManager.getString(this, player.locale).format(*arguments)
fun String.localeEventsTranslation(locale: String, vararg arguments: String) =
    skyBlockEventsResourcesManager.getString(this, locale).format(*arguments)

fun List<Hologram?>.removeViewer(player: Player) {
    forEach { hologram ->
        if(hologram == null) return@forEach

        if(!hologram.isViewer(player.uniqueId)) return@forEach
        Visibility.ManualVisibility.removeDistantViewer(hologram, player.uniqueId)

        hologram.forceHideHologram(player)
        hologram.hideHologram(player)
    }
}
fun String.translateComponentWithClickEvent(player: Player, command: String, hoverText: String, vararg args: String): Component =
    skyBlockEventsResourcesManager.getString(this, player.locale).format(*args).toColorComponent()
        .hoverEvent(HoverEvent.showText(hoverText.translate(player).toColorComponent()))
        .clickEvent(ClickEvent.runCommand("/$command"))
fun Hologram.shouldRemove(locale: String) = this.name.contains(locale)
fun Material.toItemStack(amount: Int = 1) = ItemBuilder(this, null, amount).build()

fun getRankColor(rank: Int): String {
    return when (rank) {
        1 -> "&e" // Gold
        2 -> "&6" // Orange (often used for Silver)
        3 -> "&2" // Dark Green (often used for Bronze)
        else -> "&7" // Gray
    }
}