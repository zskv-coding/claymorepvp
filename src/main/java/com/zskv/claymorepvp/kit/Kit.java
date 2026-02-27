package com.zskv.claymorepvp.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Kit {
    private final String name;
    private final Material displayItem;
    private final ItemStack[] contents;
    private final ItemStack[] armor;

    public Kit(String name, Material displayItem, ItemStack[] contents, ItemStack[] armor) {
        this.name = name;
        this.displayItem = displayItem != null ? displayItem : Material.IRON_SWORD;
        this.contents = contents;
        this.armor = armor;
    }

    public String getName() {
        return name;
    }

    public Material getDisplayItem() {
        return displayItem;
    }

    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.updateInventory();
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmor() {
        return armor;
    }
}
