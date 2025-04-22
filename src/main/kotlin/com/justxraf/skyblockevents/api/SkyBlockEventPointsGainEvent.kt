package com.justxraf.skyblockevents.api

import com.justxdude.skyblockapi.user.User
import com.justxraf.skyblockevents.events.EventType
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SkyBlockEventPointsGainEvent (
    val user: User,
    val amount: Int,
    val eventType: EventType
) : Event() {

    companion object {
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}