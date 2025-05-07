package com.justxraf.skyblockevents.events

import com.justxraf.networkapi.util.sendColoured
import com.justxdude.skyblockapi.SkyblockAPI
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxdude.skyblockapi.user.UserSettingsFlag
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.components.ComponentsManager
import com.justxraf.skyblockevents.events.data.EventData
import com.justxraf.skyblockevents.events.data.FinishedEvent
import com.justxraf.skyblockevents.events.data.active.ActiveEventData
import com.justxraf.skyblockevents.events.entities.EventEntitiesHandler
import com.justxraf.skyblockevents.users.points.PointsHandler
import com.justxraf.skyblockevents.events.regenerative.RegenerativeMaterialsHandler
import com.justxraf.skyblockevents.users.EventUserHandler
import com.justxraf.skyblockevents.util.DatabaseUtil.saveToDatabase
import com.justxraf.skyblockevents.util.eventsTranslation
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.Runnable
import net.kyori.adventure.text.Component
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ConcurrentHashMap

class EventsManager(private val componentsManager: ComponentsManager) {
    lateinit var currentEvent: Event
    var events: MutableMap<Int, EventData> = mutableMapOf()
    val editSession = mutableMapOf<UUID, EventData>()
    var finishedEvents: MutableMap<Int, FinishedEvent> = mutableMapOf()

    //db
    private val client = SkyblockAPI.instance.database.client
    private val database = client.getDatabase("skyblockevents")
    private val collection = database.getCollection("eventsmanager")
    private val eventsCollection = database.getCollection("events")
    private val finishedEventsCollection = database.getCollection("finished_events")
    private val gson = SkyblockAPI.instance.database.gson

    var eventsManagerStatus: EventsManagerStatus = EventsManagerStatus.RESTARTING

    private fun init() {
        events = loadEventsData() ?: mutableMapOf()
        finishedEvents = loadFinishedEvents() ?: mutableMapOf()

        val existingEvent = loadCurrentEvent()
        if(existingEvent == null) {
            generateNewEvent()
        } else currentEvent = existingEvent

        if(shouldFinish()) {
            eventsManagerStatus = EventsManagerStatus.RESTARTING

            currentEvent.finish()
            generateNewEvent()
        } else currentEvent.reload()

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            if(eventsManagerStatus == EventsManagerStatus.RESTARTING) return@Runnable

            saveCurrentEvent()
        }, 0L, 20 * 5)

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            if(eventsManagerStatus == EventsManagerStatus.RESTARTING) return@Runnable

            if (shouldFinish()) {
                currentEvent.end()

                generateNewEvent()
            } else eventTimeCheck()
        }, 0L, 20)

        Bukkit.getScheduler().runTaskTimer(componentsManager.plugin, Runnable {
            if(eventsManagerStatus == EventsManagerStatus.RESTARTING) return@Runnable

            events.saveToDatabase(eventsCollection)
            finishedEvents.saveToDatabase(finishedEventsCollection)
        }, 0L, 20 * 20)

    }
//    fun processFinishedEvent(finishedEvent: FinishedEvent) {
//        // Send information about points gained overall
//        // send information about most points gained
//        // Compare it to the last event
//        // Everything should be posted on discord do it's outside of this plugin.
//    }
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

    private fun loadFinishedEvents(): MutableMap<Int, FinishedEvent>? =
        supplyAsync {
            try {
                return@supplyAsync finishedEventsCollection.find().toList().map {
                    gson.fromJson(gson.toJson(it), FinishedEvent::class.java)
                }.associateByTo(mutableMapOf()) { it.id }
            } catch (e: Exception) {
                e.printStackTrace()

                return@supplyAsync null
            }
        }.exceptionally { ex ->
            println("Exception while loading finished events: ${ex.message}")
            ex.printStackTrace()

            return@exceptionally null
        }.join()

    private fun loadEventsData(): MutableMap<Int, EventData>? {
        return supplyAsync {
            return@supplyAsync eventsCollection.find().toList().map { gson.fromJson(gson.toJson(it), EventData::class.java) }
                .associateByTo(mutableMapOf()) { it.uniqueId }
        }.exceptionally { ex ->
            println("Exception while loading events data: ${ex.message}")
            ex.printStackTrace()

            return@exceptionally null
        }.join()
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
    fun generateNewEvent() {
        eventsManagerStatus = EventsManagerStatus.RESTARTING

        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable {
            if (events.isEmpty()) currentEvent = getFakeEvent()
            val event = events.values.filter { it.canBeActivated() }.random().fromData()

            event.startedAt = System.currentTimeMillis()

            currentEvent = event
            saveCurrentEvent()

            event.start()

            eventsManagerStatus = EventsManagerStatus.STARTED
        }, 20 * 30)
    }

    private fun getFakeEvent(): Event = Event(
        "",
        0,
        EventType.FISHING,
        0L,
        0L,
        mutableListOf(),
        Location(Bukkit.getWorld("world_spawn")!!, .0, .0, .0),
        RegenerativeMaterialsHandler(mutableListOf()),
        EventEntitiesHandler(),
        EventUserHandler(PointsHandler()),
        ConcurrentHashMap(),
        Pair( Location(Bukkit.getWorld("world_spawn")!!, .0, .0, .0),
            Location(Bukkit.getWorld("world_spawn")!!, .0, .0, .0)),
        0
    )

    fun saveCurrentEvent() {
        supplyAsync {
            val dataBson = Document.parse(gson.toJson(currentEvent.toData()))
            val update = Document("\$set", dataBson)

            // Delete so there's no unnecessary values if there are any
            collection.deleteOne(Filters.eq("_id", "currentEvent"))
            // save
            collection.updateOne(Filters.eq("_id", "currentEvent"), update, UpdateOptions().upsert(true))
        }.exceptionally { ex ->
            println("Exception while saving current event in EventsManager: ${ex.message}")
            ex.printStackTrace()
            null
        }
    }

    private fun loadCurrentEvent(): Event? {
        return try {
            collection.find(Filters.eq("_id", "currentEvent")).firstOrNull()
                ?.let { gson.fromJson(it.toJson(), ActiveEventData::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
            ?.toEvent()
    }

    private fun shouldFinish(): Boolean = System.currentTimeMillis() - currentEvent.endsAt > 0


//        val zone = ZoneId.of("Europe/Berlin")
//
//        val zdtLastUpdate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentEvent.startedAt), zone)
//        val zdtNow = ZonedDateTime.now(zone)
//
//        return zdtLastUpdate.get(ChronoField.DAY_OF_YEAR) != zdtNow.get(ChronoField.DAY_OF_YEAR) ||
//                zdtLastUpdate.year != zdtNow.year

    companion object {
        lateinit var instance: EventsManager
        fun initialize(componentsManager: ComponentsManager): EventsManager {
            instance = EventsManager(componentsManager)
            instance.init()

            return instance
        }
    }
}