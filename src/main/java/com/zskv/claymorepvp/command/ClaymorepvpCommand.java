package com.zskv.claymorepvp.command;

import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.kit.KitManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaymorepvpCommand implements CommandExecutor {
    private final DuelManager duelManager;
    private final KitManager kitManager;

    public ClaymorepvpCommand(DuelManager duelManager, KitManager kitManager) {
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

        if (!player.hasPermission("claymorepvp.admin")) {
            player.sendMessage(ChatUtils.format("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtils.format("&cUsage: /claymorepvp <setspawn1|setspawn2|savekit|deletekit>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("setspawn1")) {
            duelManager.setSpawn1(player.getLocation());
            player.sendMessage(ChatUtils.format("&aSpawn 1 set!"));
            return true;
        }

        if (subCommand.equals("setspawn2")) {
            duelManager.setSpawn2(player.getLocation());
            player.sendMessage(ChatUtils.format("&aSpawn 2 set!"));
            return true;
        }

        if (subCommand.equals("savekit")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.format("&cUsage: /claymorepvp savekit <name>"));
                return true;
            }
            String kitName = args[1];
            kitManager.saveKit(kitName, player.getInventory().getContents(), player.getInventory().getArmorContents());
            player.sendMessage(ChatUtils.format("&aKit '&e" + kitName + "&a' saved from your current inventory!"));
            return true;
        }

        if (subCommand.equals("deletekit")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.format("&cUsage: /claymorepvp deletekit <name>"));
                return true;
            }
            String kitName = args[1];
            if (!kitManager.kitExists(kitName)) {
                player.sendMessage(ChatUtils.format("&cKit '&e" + kitName + "&c' does not exist."));
                return true;
            }
            kitManager.deleteKit(kitName);
            player.sendMessage(ChatUtils.format("&aKit '&e" + kitName + "&a' deleted!"));
            return true;
        }

        player.sendMessage(ChatUtils.format("&cUsage: /claymorepvp <setspawn1|setspawn2|savekit|deletekit>"));
        return true;
    }
}
