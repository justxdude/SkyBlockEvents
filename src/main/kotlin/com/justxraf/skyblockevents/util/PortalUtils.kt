package com.justxraf.skyblockevents.util
import com.justxraf.skyblockevents.components.ComponentsManager
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import java.io.File
import java.io.FileInputStream
import kotlin.math.max
import kotlin.math.min

val plugin = ComponentsManager.instance.plugin
val schematicsFolder = File(plugin.dataFolder, "schematics")

private fun loadSchematic(schematicName: String): Clipboard? {
    val schematicFile = File(schematicsFolder, "${schematicName}.schem")

    if (!schematicFile.exists())
        throw NullPointerException("Schematic $schematicName is null!")

    val format = ClipboardFormats.findByFile(schematicFile)
    format?.let {
        val clipboardReader = it.getReader(FileInputStream(schematicFile))
        clipboardReader.use { reader ->
            return reader.read()
        }
    }

    return null
}
fun pasteSchematic(location: Location, filePath: String): Pair<Location, Location>? {
    val clipboard = loadSchematic(filePath)
    val region = clipboard?.region ?: return null

    // Calculate Cuboid positions for the portal
    val pos1 = getPosition(region.minimumPoint, location)
    val pos2 = getPosition(region.maximumPoint, location)

    val worldEdit = WorldEdit.getInstance()
    val session = worldEdit.newEditSessionBuilder().world(BukkitWorld(location.world)).build()
    try {
        val placementPosition = BlockVector3.at(location.x, location.y, location.z)

        // Paste the schematic
        val operation = ClipboardHolder(clipboard)
            .createPaste(session)
            .to(placementPosition)
            .ignoreAirBlocks(false)
            .build()

        Operations.complete(operation)

        return Pair(pos1.clone().add(.75, .0, .75), pos2.clone().add(.75, .0,.75))
    } finally {
        session.close()
    }
}
private fun getPosition(vector3: BlockVector3, loc: Location): Location {
    return Location(loc.world,
        vector3.x.plus(loc.x),
        vector3.y.plus(loc.y),
        vector3.z.plus(loc.z))
}
