package com.justxraf.skyblockevents.events

import com.github.supergluelib.foundation.async
import com.ibm.icu.util.TimeUnit
import com.justxdude.islandcore.utils.toLocationString
import com.justxraf.networkapi.util.Utils.sendColoured
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxraf.networkapi.util.Utils.toDate
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.custom.NetherEvent
import com.justxraf.skyblockevents.events.data.EventData
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.*
import java.util.concurrent.CompletableFuture.supplyAsync

class EventsManager(private val componentsManager: ComponentsManager) {
    var currentEvent: Event
    var events: MutableMap<Int, EventData> = mutableMapOf()
    val editSession = mutableMapOf<UUID, EventData>()

    //db
    private val client = SkyblockAPI.instance.database.client
    private val database = client.getDatabase("skyblockevents")
    private val collection = database.getCollection("eventsmanager")
    private val eventsCollection = database.getCollection("events")
    private val gson = SkyblockAPI.instance.database.gson

    init {
        currentEvent = loadCurrentEvent()?.fromData() ?: generateNewEvent()
        if (System.currentTimeMillis() - currentEvent.startedAt > 2000) {
            when (currentEvent.eventType) {
                EventType.NETHER -> (currentEvent as NetherEvent).reload()
                else -> currentEvent.reload()
            }
        }
        events = mutableMapOf()
        loadEvents()
    }

    private fun setup() {

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            saveCurrentEvent()
        }, 0L, 20 * 5)

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            if (shouldFinish()) {
                currentEvent = generateNewEvent()
                saveCurrentEvent()
            }
            eventTimeCheck()
        }, 0L, 20)

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            saveEvents()
        }, 0L, 20 * 20)

        Bukkit.getScheduler().runTaskTimerAsynchronously(componentsManager.plugin, Runnable {
            // Don't send if:
            // player is in the current event world, player doesn't have high enough level, player set notifications to false.
            val message = listOf(
                "&9&m-".repeat(30),
                "&a&lWydarzenie ${currentEvent.name} &atrwa!",
                "&7",
                "&7Dołącz do wydarzenia poprzez portal na spawnie (${currentEvent.portalLocation?.toLocationString()} (X,Y,Z)",
                "&7I zdobądź surowce które nie są normalnie dostępne!",
                "&7Te wydarzenie kończy się o ${currentEvent.endsAt.toDate()}.",
                "&9&m-".repeat(30)
            )
        }, 0L, 20 * 240)
    }

    private fun eventTimeCheck() {
        async {
            val timeLeft = currentEvent.endsAt - System.currentTimeMillis()
            val timeMessages = mapOf(
                1L to "sekundę",
                2L to "2 sekundy",
                3L to "3 sekundy",
                4L to "4 sekundy",
                5L to "5 sekund",
                6L to "6 sekund",
                7L to "7 sekund",
                8L to "8 sekund",
                9L to "9 sekund",
                10L to "10 sekund",
                60L to "minutę",
                900L to "15 minut",
                1800L to "30 minut",
                3600L to "godzinę"
            )

            val message = "Wydarzenie ${currentEvent.name} kończy się za %time!"

            val messageToSend = when (val timeLeftSeconds = timeLeft / 1000) {
                in 1..10 -> "&c$message".replace("%time", timeMessages[timeLeftSeconds] ?: "")
                60L -> "&c$message".replace("%time", timeMessages[60L] ?: "")
                900L -> "&c$message".replace("%time", timeMessages[900L] ?: "")
                1800L -> "&7$message".replace("%time", timeMessages[1800L] ?: "")
                3600L -> "&7$message".replace("%time", timeMessages[3600L] ?: "")
                else -> ""
            }

            if (messageToSend.isNotEmpty()) {
                Bukkit.getOnlinePlayers().forEach {
                    it.sendColoured(messageToSend)
                }
            }
        }
    }

    private fun loadEvents() {
        supplyAsync {
            try {
                val documents = eventsCollection.find().toList()
                documents.forEach { document ->
                    val eventJson = gson.toJson(document)
                    val event = gson.fromJson(eventJson, EventData::class.java)

                    events[event.uniqueId] = event
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.exceptionally { ex ->
            println("Exception in async task: ${ex.message}")
            ex.printStackTrace()
        }
    }

    fun saveEvent(eventData: EventData) {
        supplyAsync {
            try {
                val dataBson = Document.parse(gson.toJson(eventData))
                val update = Document("\$set", dataBson)
                eventsCollection.updateOne(Filters.eq("_id", eventData.uniqueId), update, UpdateOptions().upsert(true))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.exceptionally { ex ->
            println("Exception in async task: ${ex.message}")
            ex.printStackTrace()
            null
        }
    }

    private fun saveEvents() {
        if (events.isEmpty()) return

        supplyAsync {
            val bulkWriteRequests = events.map { it.key to it.value }.map { (id, event) ->
                val json = gson.toJson(event)
                val document = Document.parse(json)
                val filter = Document("_id", id)
                UpdateOneModel<Document>(filter, Document("\$set", document), UpdateOptions().upsert(true))
            }
            eventsCollection.bulkWrite(bulkWriteRequests)
        }.exceptionally { ex ->
            println("Exception in async task: ${ex.message}")
            ex.printStackTrace()
            null
        }
    }

    private fun generateNewEvent(): Event {
        if (currentEvent != null) currentEvent.end()

        if (events.isEmpty()) return Event(
            "",
            0,
            EventType.FISHING,
            0,
            0,
            mutableListOf(""),
            Location(Bukkit.getWorld("world_spawn")!!, .0, .0, .0)
        ) // As a debug if there are no events in the database

        val event = events.values.random().fromData()
        event.start()
        currentEvent = event
        saveCurrentEvent()

        return event
    }

    private fun saveCurrentEvent() {
        supplyAsync {
            val dataBson = Document.parse(gson.toJson(currentEvent.toData()))
            val update = Document("\$set", dataBson)

            collection.updateOne(Filters.eq("_id", "currentEvent"), update, UpdateOptions().upsert(true))
        }.exceptionally { ex ->
            println("Exception in async task: ${ex.message}")
            ex.printStackTrace()
            null
        }
    }

    private fun loadCurrentEvent(): EventData? {
        try {
            return collection.find(Filters.eq("_id", "currentEvent")).firstOrNull()
                ?.let { gson.fromJson(it.toJson(), EventData::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun shouldFinish(): Boolean {
        val zone = ZoneId.of("Europe/Berlin")

        val zdtLastUpdate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentEvent.startedAt), zone)
        val zdtNow = ZonedDateTime.now(zone)

        return zdtLastUpdate.get(ChronoField.DAY_OF_YEAR) != zdtNow.get(ChronoField.DAY_OF_YEAR) ||
                zdtLastUpdate.year != zdtNow.year
    }

    companion object {
        lateinit var instance: EventsManager
        fun initialize(componentsManager: ComponentsManager): EventsManager {
            instance = EventsManager(componentsManager)
            instance.setup()

            return instance
        }
    }
}