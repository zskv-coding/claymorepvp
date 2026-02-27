package com.zskv.claymorepvp.listener;

import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private final DuelManager duelManager;

    public GuiListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.contains("Duel Request: ")) {
            event.setCancelled(true);
            // Strip colors to get name
            String challengerName = org.bukkit.ChatColor.stripColor(title).replace("Duel Request: ", "");
            Player challenger = Bukkit.getPlayer(challengerName);

            if (clicked.getType() == Material.LIME_WOOL) {
                player.closeInventory();
                if (challenger == null || !challenger.isOnline()) {
                    player.sendMessage(ChatUtils.format("&cChallenger is no longer online."));
                    return;
                }
                duelManager.acceptRequest(player, challenger);
            } else if (clicked.getType() == Material.RED_WOOL) {
                player.closeInventory();
                player.sendMessage(ChatUtils.format("&cYou declined the duel request."));
                if (challenger != null && challenger.isOnline()) {
                    challenger.sendMessage(ChatUtils.format("&c" + player.getName() + " declined your duel request."));
                }
            }
        } else if (title.contains("Select Kit for: ")) {
            event.setCancelled(true);
            String targetName = org.bukkit.ChatColor.stripColor(title).replace("Select Kit for: ", "");
            Player target = Bukkit.getPlayer(targetName);

            if (target == null || !target.isOnline()) {
                player.closeInventory();
                player.sendMessage(ChatUtils.format("&cTarget is no longer online."));
                return;
            }

            if (clicked.getType() == Material.IRON_SWORD) {
                String kitName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                player.openInventory(com.zskv.claymorepvp.gui.DuelGUI.createConfirmDuelGUI(target, kitName));
            }
        } else if (title.contains("Duel: ")) {
            event.setCancelled(true);
            // Format: Duel: TargetName - KitName
            String stripped = org.bukkit.ChatColor.stripColor(title).replace("Duel: ", "");
            String[] parts = stripped.split(" - ");
            if (parts.length < 2) return;

            String targetName = parts[0];
            String kitName = parts[1];
            Player target = Bukkit.getPlayer(targetName);

            if (clicked.getType() == Material.LIME_WOOL) {
                player.closeInventory();
                if (target == null || !target.isOnline()) {
                    player.sendMessage(ChatUtils.format("&cTarget is no longer online."));
                    return;
                }
                duelManager.sendRequest(player, target, kitName);
            } else if (clicked.getType() == Material.RED_WOOL) {
                player.closeInventory();
                player.sendMessage(ChatUtils.format("&cDuel request cancelled."));
            }
        }
    }
}
