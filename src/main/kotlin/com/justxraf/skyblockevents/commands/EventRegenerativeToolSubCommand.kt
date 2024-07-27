package com.justxraf.skyblockevents.commands

import com.github.supergluelib.foundation.util.ItemBuilder
import com.justxraf.networkapi.util.Utils.sendColoured
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

object EventRegenerativeToolSubCommand {
    /*

    /event getregenerative <plant/block>

     */
    private fun shouldProcess(player: Player, args: Array<String>): ToolType? {
        if(args.size != 2) {
            player.sendColoured("&cZbyt mało argumentów! Użyj /event getregenerative <plant/block>.")
            return null
        }
        val type = ToolType.entries.firstOrNull { it.name == args[1].uppercase() }

        if(type == null) {
            player.sendColoured("&cPodałeś(aś) zły typ! Użyj \"plant\" lub \"block\".")
            return null
        }
        return type
    }
    fun process(player: Player, args: Array<String>) {
        val type = shouldProcess(player, args) ?: return
        when(type) {
            ToolType.BLOCK -> {
                val item = ItemBuilder(Material.WOODEN_PICKAXE, "&cNarzędzie do modyfikacji regenerujących bloków")
                    .itemName("regenerative_block_editor")
                    .lore(listOf("&7Kliknij lewym na blok", "&7Aby usunąć ją z listy.", "&7", "&7Kliknij prawym na blok", "&7Aby dodać go do listy."))
                    .hideEnchants(true)
                    .addEnchant(Enchantment.PROTECTION, 1)
                    .build()
                player.inventory.setItem(EquipmentSlot.HAND, item)

                player.sendColoured("&7Otrzymałeś przedmiot do modyfikowania regenerujących bloków. " +
                        "Możesz go używać tylko w światach w których są wydarzenia.")
            }
            ToolType.PLANT -> {
                val item = ItemBuilder(Material.WOODEN_HOE, "&cNarzędzie do modyfikacji regenerujących roślin")
                    .itemName("regenerative_plant_editor")
                    .lore(listOf("&7Kliknij lewym na roślinę", "&7Aby usunąć ją z listy.", "&7", "&7Kliknij prawym na roślinę", "&7Aby dodać ją do listy."))
                    .hideEnchants(true)
                    .addEnchant(Enchantment.PROTECTION, 1)
                    .build()
                player.inventory.setItem(EquipmentSlot.HAND, item)

                player.sendColoured("&7Otrzymałeś przedmiot do modyfikowania regenerujących roślin. " +
                        "Możesz go używać tylko w światach w których są wydarzenia.")

            }
        }
    }

}
private enum class ToolType{
    PLANT, BLOCK
}