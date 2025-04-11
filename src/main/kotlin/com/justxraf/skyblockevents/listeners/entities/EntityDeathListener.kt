package com.justxraf.skyblockevents.listeners.entities

import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.EventsManager
import de.oliver.fancyholograms.api.FancyHologramsPlugin
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class EntityDeathListener : Listener {
    private val eventsManager = EventsManager.instance
    private val hologramManager = FancyHologramsPlugin.get().hologramManager

    @EventHandler
    fun onEntityMove(event: EntityMoveEvent) {
        if(event.entity.location.y > .0) return

        val currentEvent = eventsManager.currentEvent
        val world = currentEvent.spawnLocation.world

        val entity = event.entity
        val entityWorld = entity.world

        if(world != entityWorld) return

        Bukkit.getPluginManager().callEvent(EntityDeathEvent(entity, DamageSource.builder(DamageType.FALL).build(), mutableListOf()))
        entity.remove()
    }
    @EventHandler
    fun onActiveMobDeath(event: MythicMobDeathEvent) {
        val killer = event.killer ?: return
        if(killer.uniqueId == event.mob.uniqueId) {
            event.mob.remove()
            return
        }
    }
    @EventHandler
    fun onEntityDeath(event: MythicMobDeathEvent) {
        val killer = event.killer ?: return
        if(killer.uniqueId == event.mob.uniqueId) {
            return
        }

        val activeMob = event.mob ?: return

        val currentEvent = eventsManager.currentEvent
        val world = currentEvent.spawnLocation.world ?: return

        if(activeMob.entity.world != world) return

        Bukkit.getScheduler().runTaskLater(SkyBlockEvents.instance, Runnable {
            currentEvent.eventEntitiesManager.removeEntity(activeMob)
        }, 10)
    }
}