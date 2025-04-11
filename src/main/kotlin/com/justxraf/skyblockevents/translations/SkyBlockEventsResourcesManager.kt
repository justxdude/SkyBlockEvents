package com.justxraf.skyblockevents.translations

import com.justxraf.questscore.resources.QuestsResourcesManager
import java.util.ResourceBundle

class SkyBlockEventsResourcesManager {
    private var pl_pl: Map<String, String> = mapOf()
    private var en_us: Map<String, String> = mapOf()
    val languages = listOf("en_us", "pl_pl")

    private fun setup() {
        pl_pl = loadProperties("messages_pl_pl")
        en_us = loadProperties("messages_en_us")
    }
    private fun loadProperties(baseName: String): Map<String, String> {
        val resourceBundle = ResourceBundle.getBundle(baseName)
        return resourceBundle.keySet().associateWith { resourceBundle.getString(it) }
    }
    fun getString(key: String, language: String): String {
        return when (language) {
            "pl_pl" -> pl_pl[key] ?: key
            else -> en_us[key] ?: key
        }
    }
    companion object {
        val instance: SkyBlockEventsResourcesManager by lazy {
            SkyBlockEventsResourcesManager().apply { setup() }
        }
    }
}