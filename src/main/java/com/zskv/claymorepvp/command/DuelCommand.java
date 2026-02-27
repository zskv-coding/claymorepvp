package com.zskv.claymorepvp.command;

import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.kit.KitManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {
    private final DuelManager duelManager;
    private final KitManager kitManager;

    public DuelCommand(DuelManager duelManager, KitManager kitManager) {
        this.duelManager = duelManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.format("&cThis command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatUtils.format("&cUsage: /duel <player>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.format("&cUsage: /duel accept <player>"));
                return true;
            }

            Player challenger = Bukkit.getPlayer(args[1]);
            if (challenger == null || !challenger.isOnline()) {
                player.sendMessage(ChatUtils.format("&cPlayer not found."));
                return true;
            }

            duelManager.acceptRequest(player, challenger);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatUtils.format("&cPlayer not found."));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatUtils.format("&cYou cannot duel yourself!"));
            return true;
        }

        player.openInventory(com.zskv.claymorepvp.gui.DuelGUI.createKitSelectionGUI(target, kitManager));
        return true;
    }
}
