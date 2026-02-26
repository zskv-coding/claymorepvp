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
        if (!title.contains("Duel Request: ")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

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
    }
}
