package com.zskv.claymorepvp.util;

import org.bukkit.ChatColor;

public class ChatUtils {

    private static final String PREFIX = "&8[&6Claymore&ePvP&8] &r";

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', PREFIX + message);
    }

    public static String formatNoPrefix(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
