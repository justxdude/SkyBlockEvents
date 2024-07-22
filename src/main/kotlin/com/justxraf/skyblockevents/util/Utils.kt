package com.justxraf.skyblockevents.util

import org.bukkit.Location
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

fun pushPlayerIfClose(targetLocation: Location, player: Player, threshold: Double, pushStrength: Double) {
    val playerLocation = player.location

    val distance = playerLocation.distance(targetLocation)

    if (distance <= threshold) {
        val direction = playerLocation.toVector().subtract(targetLocation.toVector()).normalize()

        val velocity = direction.multiply(pushStrength)
        player.velocity = velocity
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
        parts.add("" +
                "${years} year")
    }
    else if (years > 0 && months > 0 && days.toInt() == 0) {
        parts.add("" +
                "$years ${if(years > 1) " years, " else " year, "} " +
                "$months ${if (months > 1) " months" else " month"}")
    }
    else if (years > 0 && months > 0 && days > 0) {
        parts.add("" +
                "$years ${if(years > 1) " years, " else " year, "} " +
                "$months ${if (months > 1) " months, " else " month, "}" +
                "$days ${if (days > 1) " days" else " day"}")
    }
    else if (months > 0 && days.toInt() == 0) {
        parts.add("" +
                "$months month")
    }
    else if (months > 0 && days > 0) {
        parts.add("" +
                "$months ${if(months > 1) " months, " else " month, "} " +
                "$days ${if (days > 1) " days" else " day"}")
    }
    else if (days.toInt() == 1 && hours.toInt() == 0) {
        parts.add("" +
                "$days day")
    }
    else if (days > 0 && hours > 0) {
        parts.add("" +
                "$days ${if(days > 1) " days, " else " day, "} " +
                "$hours ${if (hours > 1) " hours" else " hour"}")
    }
    else if (hours.toInt() == 1) {
        parts.add("" +
                "$hours hour")
    }
    else if (hours > 0 && minutes > 0) {
        parts.add("" +
                "$hours ${if(hours > 1) " hours, " else " hour, "}" +
                "$minutes ${if (minutes > 1) " minutes" else "minute"}")
    }
    else if (minutes.toInt() == 1 && seconds.toInt() == 0) {
        parts.add("" +
                "$minutes minute")
    }
    else if (minutes > 0 && seconds > 0) {
        parts.add("" +
                "$minutes ${if(minutes > 1) " minutes, " else " minut, "} " +
                "$seconds ${if(seconds > 1) " seconds" else "second"}")
    }
    return parts.joinToString(" ")
}