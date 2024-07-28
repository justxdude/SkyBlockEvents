package com.justxraf.skyblockevents.util

import com.justxraf.networkapi.util.Utils.sendColoured
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
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

fun Long.formatDuration(): String {
    val currentTime = System.currentTimeMillis()
    val durationMillis = currentTime - this

    val years = TimeUnit.MILLISECONDS.toDays(durationMillis) / 365
    val months = TimeUnit.MILLISECONDS.toDays(durationMillis) / 30 % 12
    val days = TimeUnit.MILLISECONDS.toDays(durationMillis) % 7
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

    val parts = mutableListOf<String>()


    if (years > 0 && months.toInt() == 0 && days.toInt() == 0) {
        parts.add(
            "" +
                    "${years} year"
        )
    } else if (years > 0 && months > 0 && days.toInt() == 0) {
        parts.add(
            "" +
                    "$years ${if (years > 1) " years, " else " year, "} " +
                    "$months ${if (months > 1) " months" else " month"}"
        )
    } else if (years > 0 && months > 0 && days > 0) {
        parts.add(
            "" +
                    "$years ${if (years > 1) " years, " else " year, "} " +
                    "$months ${if (months > 1) " months, " else " month, "}" +
                    "$days ${if (days > 1) " days" else " day"}"
        )
    } else if (months > 0 && days.toInt() == 0) {
        parts.add(
            "" +
                    "$months month"
        )
    } else if (months > 0 && days > 0) {
        parts.add(
            "" +
                    "$months ${if (months > 1) " months, " else " month, "} " +
                    "$days ${if (days > 1) " days" else " day"}"
        )
    } else if (days.toInt() == 1 && hours.toInt() == 0) {
        parts.add(
            "" +
                    "$days day"
        )
    } else if (days > 0 && hours > 0) {
        parts.add(
            "" +
                    "$days ${if (days > 1) " days, " else " day, "} " +
                    "$hours ${if (hours > 1) " hours" else " hour"}"
        )
    } else if (hours.toInt() == 1) {
        parts.add(
            "" +
                    "$hours hour"
        )
    } else if (hours > 0 && minutes > 0) {
        parts.add(
            "" +
                    "$hours ${if (hours > 1) " hours, " else " hour, "}" +
                    "$minutes ${if (minutes > 1) " minutes" else "minute"}"
        )
    } else if (minutes.toInt() == 1 && seconds.toInt() == 0) {
        parts.add(
            "" +
                    "$minutes minute"
        )
    } else if (minutes > 0 && seconds > 0) {
        parts.add(
            "" +
                    "$minutes ${if (minutes > 1) " minutes, " else " minut, "} " +
                    "$seconds ${if (seconds > 1) " seconds" else "second"}"
        )
    }
    return parts.joinToString(" ")
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

fun Location.isInCuboid(pos1: Location, pos2: Location): Boolean {
    val tolerance = 0.5
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

enum class SelectionAnswer {
    NULL_POS1, NULL_POS2, NULL, CORRECT
}