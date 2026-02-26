package com.zskv.claymorepvp.command;

import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.kit.KitManager;
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
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /duel <player> [kit] or /duel accept <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /duel accept <player>");
                return true;
            }

            Player challenger = Bukkit.getPlayer(args[1]);
            if (challenger == null || !challenger.isOnline()) {
                player.sendMessage("Player not found.");
                return true;
            }

            duelManager.acceptRequest(player, challenger);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("Player not found.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("You cannot duel yourself!");
            return true;
        }

        String kitName = null;
        if (args.length >= 2) {
            kitName = args[1];
            if (!kitManager.kitExists(kitName)) {
                player.sendMessage("Kit '" + kitName + "' does not exist!");
                return true;
            }
        }

        duelManager.sendRequest(player, target, kitName);
        return true;
    }
}
