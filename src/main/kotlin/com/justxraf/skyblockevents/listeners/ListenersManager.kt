package com.justxraf.skyblockevents.listeners

import com.github.supergluelib.foundation.registerListeners
import com.justxdude.skyblockapi.user.UserExtensions.asUser
import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventsManager
import com.justxraf.skyblockevents.listeners.npcs.QuestNPCInteractListener
import com.justxraf.skyblockevents.listeners.blocks.RegenerativeBlockListener
import com.justxraf.skyblockevents.listeners.entities.*
import com.justxraf.skyblockevents.listeners.fishing.PlayerFishingListener
import com.justxraf.skyblockevents.listeners.plants.RegenerativePlantListener
import com.justxraf.skyblockevents.listeners.npcs.QuestNpcPlayerNearbyListener
import com.justxraf.skyblockevents.listeners.plants.RegenerativePlantGrowListener
import com.justxraf.skyblockevents.listeners.players.PlayerChatClickListener
import com.justxraf.skyblockevents.listeners.players.PlayerDeathListener
import com.justxraf.skyblockevents.listeners.players.PlayerJoinListener
import com.justxraf.skyblockevents.listeners.players.PlayerMoveListener
import com.justxraf.skyblockevents.listeners.players.PlayerQuitListener
import com.justxraf.skyblockevents.listeners.portals.PortalBreakListener
import com.justxraf.skyblockevents.listeners.portals.PortalJoinListener
import com.justxraf.skyblockevents.util.skyBlockEventsResourcesManager
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class ListenersManager(private val plugin: JavaPlugin) {
    fun doChecks(entityLocation: Location, eventLocation: Location): Boolean = entityLocation.world == eventLocation.world

    private fun setup() {
        // Blocks
        plugin.registerListeners(
            RegenerativeBlockListener()
        )
        // Entities
        plugin.registerListeners(
            EntityDamageListener(),
            EntityDeathListener(),
            NaturalEntitySpawnListener(),
            EntityTargetPlayerListener(),
            EntityTargetEntityListener(),
            EntityBlockBreakListener(),
        )
        // NPCs
        plugin.registerListeners(
            QuestNPCInteractListener(),
            QuestNpcPlayerNearbyListener(),
            EntityPortalListener(),

        )
        // Plants
        plugin.registerListeners(
            RegenerativePlantListener(),
            RegenerativePlantGrowListener(),
        )
        // Players
        plugin.registerListeners(
            PlayerDeathListener(),
            PlayerJoinListener(),
            PlayerQuitListener(),
            PlayerFishingListener(),
            PlayerMoveListener(),
            PlayerChatClickListener()
        )
        plugin.registerListeners(
            PortalBreakListener(),
            PortalJoinListener(),
        )

        Bukkit.getScheduler().runTaskTimer(SkyBlockEvents.instance, Runnable {
            checkHolograms()
        }, 0, 40)
    }
    private fun checkHolograms() {
        val players = Bukkit.getOnlinePlayers()
        val currentEvent = EventsManager.instance.currentEvent
        val hologramManager = FancyHologramsPlugin.get().hologramManager
        players.forEach { player ->
            val user = player.asUser() ?: return

            currentEvent.portals?.values?.forEach { portal ->
                portal.removeDistantViewerFromHologram(player,user, skyBlockEventsResourcesManager, hologramManager)
                portal.showHologram(player, user, hologramManager, skyBlockEventsResourcesManager)
            }
            currentEvent.removeDistantViewerFromNPCHologram(player, skyBlockEventsResourcesManager, hologramManager)
            currentEvent.showNPCHologram(player, hologramManager, skyBlockEventsResourcesManager)
        }
    }

    companion object {
        lateinit var instance: ListenersManager
        fun initialize(plugin: SkyBlockEvents): ListenersManager {
            instance = ListenersManager(plugin)
            instance.setup()
            return instance
        }
    }
}