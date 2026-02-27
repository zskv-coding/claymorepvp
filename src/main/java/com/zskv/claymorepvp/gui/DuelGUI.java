package com.zskv.claymorepvp.gui;

import com.zskv.claymorepvp.kit.Kit;
import com.zskv.claymorepvp.kit.KitManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DuelGUI {

    public static Inventory createKitSelectionGUI(Player target, KitManager kitManager) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtils.formatNoPrefix("&8Select Kit for: &6" + target.getName()));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        int[] slots = {10, 12, 14, 16};
        int i = 0;
        for (Kit kit : kitManager.getKits()) {
            if (i >= slots.length) break;
            
            ItemStack kitItem = new ItemStack(kit.getDisplayItem());
            ItemMeta kitMeta = kitItem.getItemMeta();
            if (kitMeta != null) {
                kitMeta.setDisplayName(ChatUtils.formatNoPrefix("&6&l" + kit.getName().toUpperCase()));
                List<String> lore = new ArrayList<>();
                lore.add(ChatUtils.formatNoPrefix("&7Click to select this kit!"));
                kitMeta.setLore(lore);
                kitItem.setItemMeta(kitMeta);
            }
            inv.setItem(slots[i++], kitItem);
        }

        return inv;
    }

    public static Inventory createConfirmDuelGUI(Player target, String kitName) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtils.formatNoPrefix("&8Duel: &6" + target.getName() + " &8- &e" + kitName.toUpperCase()));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatUtils.formatNoPrefix("&a&lSEND REQUEST"));
            confirmMeta.setLore(Collections.singletonList(ChatUtils.formatNoPrefix("&7Click to send the duel request!")));
            confirm.setItemMeta(confirmMeta);
        }

        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatUtils.formatNoPrefix("&c&lCANCEL"));
            cancelMeta.setLore(Collections.singletonList(ChatUtils.formatNoPrefix("&7Click to cancel.")));
            cancel.setItemMeta(cancelMeta);
        }

        inv.setItem(11, confirm);
        inv.setItem(15, cancel);

        return inv;
    }

    public static Inventory createRequestGUI(Player challenger) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtils.formatNoPrefix("&8Duel Request: &6" + challenger.getName()));

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
            acceptMeta.setDisplayName(ChatUtils.formatNoPrefix("&a&lACCEPT"));
            acceptMeta.setLore(Collections.singletonList(ChatUtils.formatNoPrefix("&7Click to accept the duel!")));
            accept.setItemMeta(acceptMeta);
        }

        ItemStack deny = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = deny.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName(ChatUtils.formatNoPrefix("&c&lDENY"));
            denyMeta.setLore(Collections.singletonList(ChatUtils.formatNoPrefix("&7Click to decline the duel.")));
            deny.setItemMeta(denyMeta);
        }

        inv.setItem(11, accept);
        inv.setItem(15, deny);

        return inv;
    }
}
