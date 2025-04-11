package com.justxraf.skyblockevents.events.regenerative

import com.justxraf.skyblockevents.SkyBlockEvents
import com.justxraf.skyblockevents.events.Event
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class RegenerativeMaterialsManager(
    var regenerativeMaterials: MutableList<RegenerativeMaterial> = mutableListOf(),
    private var task: BukkitTask? = null,
) {
    @Transient
    private var materialsByMaterial: MutableMap<Material, RegenerativeMaterial> = mutableMapOf()

    fun setup(event: Event) {
        assignMaterials()

        reloadRegenerativeMaterials()
        startTask(event)
    }
    private fun assignMaterials() {
        materialsByMaterial = mutableMapOf()
        regenerativeMaterials.associateByTo(materialsByMaterial) { it.material }
    }

    fun reload(event: Event) {
        stop()
        setup(event)
    }

    fun startTask(event: Event) {
        task = object : BukkitRunnable() {
            override fun run() {
                checkRegenerativeMaterials(event)
            }
        }.runTaskTimer(SkyBlockEvents.instance, 0, 20 * 1)
    }
    fun stop() {
        task?.cancel()
    }
    fun addRegenerativeMaterial(
        material: Material,
        isHarvestable: Boolean,
    ) {
        if(isRegenerative(material)) return

        val newMaterial = RegenerativeMaterial(material, isHarvestable)
        newMaterial.setup()

        regenerativeMaterials.add(newMaterial)
        materialsByMaterial.put(material, newMaterial)
    }

    fun removeMaterial(material: Material) {
        if(!isRegenerative(material)) return

        val regenerativeMaterial = materialsByMaterial[material] ?: return
        regenerativeMaterial.end()

        materialsByMaterial.remove(material)
        regenerativeMaterials.remove(regenerativeMaterial)
    }
    fun isRegenerative(material: Material): Boolean =
        materialsByMaterial[material] != null

    fun breakRegenerativeMaterial(location: Location, material: Material) {
        val regenerativeMaterial = materialsByMaterial[material] ?: return

        regenerativeMaterial.breakMaterial(location)
    }
    fun canBreakMaterialAt(location: Location, material: Material): Boolean {
        val regenerativeMaterial = materialsByMaterial[material] ?: return false
        return regenerativeMaterial.canBreak(location)
    }
    private fun reloadRegenerativeMaterials() {
        regenerativeMaterials.forEach { it.reload() }
    }
    private fun checkRegenerativeMaterials(event: Event) {
        regenerativeMaterials.forEach { it.checkBrokenBlocks(event) }
    }
}