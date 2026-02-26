package com.zskv.claymorepvp.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class DuelGUI {

    public static Inventory createRequestGUI(Player challenger) {
        Inventory inv = Bukkit.createInventory(null, 27, "Duel Request: " + challenger.getName());

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        ItemStack accept = new ItemStack(Material.LIME_WOOL);
        ItemMeta acceptMeta = accept.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName("§a§lACCEPT");
            acceptMeta.setLore(Collections.singletonList("§7Click to accept the duel!"));
            accept.setItemMeta(acceptMeta);
        }

        ItemStack deny = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = deny.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName("§c§lDENY");
            denyMeta.setLore(Collections.singletonList("§7Click to decline the duel."));
            deny.setItemMeta(denyMeta);
        }

        inv.setItem(11, accept);
        inv.setItem(15, deny);

        return inv;
    }
}
