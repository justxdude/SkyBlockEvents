package com.justxraf.skyblockevents.events

import com.justxraf.networkapi.util.sendColoured
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserSettingsFlag
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.events.data.FinishedEvent
import com.justxraf.skyblockevents.events.entities.EventEntitiesHandler
import com.justxraf.skyblockevents.users.points.PointsHandler
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterialsHandler
import com.justxraf.skyblockevents.users.EventUserHandler
import com.justxraf.skyblockevents.util.eventsTranslation
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.Runnable
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.*
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ConcurrentHashMap

class EventsManager(private val componentsManager: ComponentsManager) {
    lateinit var currentEvent: Event
    var events: MutableMap<Int, EventData> = mutableMapOf()
    val editSession = mutableMapOf<UUID, EventData>()
    val finishedEvents: MutableMap<UUID, FinishedEvent> = mutableMapOf()

    //db
    // TODO add a new collection for finished events
    private val client = SkyblockAPI.instance.database.client
    private val database = client.getDatabase("skyblockevents")
    private val collection = database.getCollection("eventsmanager")
    private val eventsCollection = database.getCollection("events")
    private val gson = SkyblockAPI.instance.database.gson

    private fun setup() {
        events = mutableMapOf()
        loadEvents()

        currentEvent = loadCurrentEvent()?.fromData() ?: generateNewEvent()

        if(shouldFinish()) {
            currentEvent = generateNewEvent()
            saveCurrentEvent()
        } else {
            currentEvent.reload()
        }

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            saveCurrentEvent()
        }, 0L, 20 * 5)

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            if (shouldFinish()) {
                currentEvent.end()

                currentEvent = generateNewEvent()
                saveCurrentEvent()
                return@Runnable
            }
            eventTimeCheck()
        }, 0L, 20)

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            saveEvents()
        }, 0L, 20 * 20)
    }
    private val timeMessages = mapOf(
        1L to "second",
        2L to "seconds",
        3L to "seconds",
        4L to "seconds",
        5L to "seconds2",
        6L to "seconds2",
        7L to "seconds2",
        8L to "seconds2",
        9L to "seconds2",
        10L to "seconds2",
        60L to "minute",
        900L to "minutes",
        1800L to "minutes",
        3600L to "hour"
    )

    private fun eventTimeCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(componentsManager.plugin, Runnable {
            currentEvent.let { currentEvent ->
                val timeLeft = currentEvent.endsAt - System.currentTimeMillis()

                val message = "event.ends.in"
                Bukkit.getOnlinePlayers().filter {
                    val user = it.asUser()!!
                    user.level >= currentEvent.requiredLevel
                            && user.getFlagBoolean(UserSettingsFlag.ALLOW_EVENT_NOTIFICATIONS)
                }.forEach { player ->
                    val translatedMessage = message.eventsTranslation(
                        player,
                        "nether".eventsTranslation(player)
                    )

                    val messageToSend = when (val timeLeftSeconds = timeLeft / 1000) {
                        in 1..10 -> "&c$translatedMessage".replace(
                            "#time",
                            timeMessages[timeLeftSeconds]?.eventsTranslation(
                                player,
                                timeLeftSeconds.toString()
                            ) ?: ""
                        )

                        60L -> "&c$translatedMessage".replace(
                            "#time",
                            timeMessages[60L]?.eventsTranslation(player, "60") ?: ""
                        )

                        900L -> "&c$translatedMessage".replace(
                            "#time",
                            timeMessages[900L]?.eventsTranslation(player, "15") ?: ""
                        )

                        1800L -> "&7$translatedMessage".replace(
                            "#time",
                            timeMessages[1800L]?.eventsTranslation(player, "30") ?: ""
                        )

                        3600L -> "&7$translatedMessage".replace(
                            "#time",
                            timeMessages[3600L]?.eventsTranslation(player) ?: ""
                        )

                        else -> ""
                    }
                    if (messageToSend.isNotEmpty())
                        player.sendColoured(messageToSend)

                }
            }
        })
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
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")
        println("Generating new event!")

        if (events.isEmpty()) return Event(
            "",
            0,
            EventType.FISHING,
            0,
            0,
            mutableListOf(""),
            Location(Bukkit.getWorld("world_spawn")!!, .0, .0, .0),
            RegenerativeMaterialsHandler(),
            EventEntitiesHandler(),
            EventUserHandler(PointsHandler(), ConcurrentHashMap())
        ) // As a debug if there are no events in the database

        val event = events.values.random().fromData()

        event.startedAt = System.currentTimeMillis()
        event.start()

        currentEvent = event
        saveCurrentEvent()

        return event
    }

    fun saveCurrentEvent() {
        supplyAsync {
            val dataBson = Document.parse(gson.toJson(currentEvent?.toData()))
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