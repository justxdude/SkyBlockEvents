package com.justxraf.skyblockevents.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

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