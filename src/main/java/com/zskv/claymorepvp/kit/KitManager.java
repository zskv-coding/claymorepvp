package com.zskv.claymorepvp.kit;

import com.zskv.claymorepvp.Claymorepvp;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitManager {
    private final Claymorepvp plugin;
    private final Map<String, Kit> kits = new HashMap<>();

    public KitManager(Claymorepvp plugin) {
        this.plugin = plugin;
        loadKits();
    }

    public void loadKits() {
        kits.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kits");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            List<ItemStack> contentsList = (List<ItemStack>) section.get(key + ".contents");
            List<ItemStack> armorList = (List<ItemStack>) section.get(key + ".armor");

            ItemStack[] contents = contentsList != null ? contentsList.toArray(new ItemStack[0]) : new ItemStack[0];
            ItemStack[] armor = armorList != null ? armorList.toArray(new ItemStack[0]) : new ItemStack[0];

            Material displayItem = Material.IRON_SWORD;
            String materialName = section.getString(key + ".display-item");
            if (materialName != null) {
                try {
                    displayItem = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            kits.put(key.toLowerCase(), new Kit(key, displayItem, contents, armor));
        }
    }

    public void saveKit(String name, ItemStack[] contents, ItemStack[] armor) {
        String path = "kits." + name.toLowerCase();
        plugin.getConfig().set(path + ".contents", contents);
        plugin.getConfig().set(path + ".armor", armor);
        plugin.saveConfig();
        kits.put(name.toLowerCase(), new Kit(name, Material.IRON_SWORD, contents, armor));
    }

    public void deleteKit(String name) {
        plugin.getConfig().set("kits." + name.toLowerCase(), null);
        plugin.saveConfig();
        kits.remove(name.toLowerCase());
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    public boolean kitExists(String name) {
        return kits.containsKey(name.toLowerCase());
    }
}
