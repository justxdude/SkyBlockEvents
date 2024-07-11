package com.justxraf.skyblockevents.components

import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.ListenersManager

class ComponentsManager(val plugin: SkyBlockEvents) {
    private fun setup() {
        instance = this

        EventsManager.initialize(this)
        ListenersManager.initialize(plugin)

    }
    companion object {
        lateinit var instance: ComponentsManager
        fun initialize(plugin: SkyBlockEvents): ComponentsManager {
            instance = ComponentsManager(plugin)
            instance.setup()
            return instance
        }
    }
}