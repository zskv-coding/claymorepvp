package com.zskv.claymorepvp.leaderboard;

import com.zskv.claymorepvp.Claymorepvp;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class LeaderboardManager {

    private final Claymorepvp plugin;
    private final Location location;
    private TextDisplay leaderboardDisplay;
    private BukkitTask updateTask;

    public LeaderboardManager(Claymorepvp plugin) {
        this.plugin = plugin;
        World world = Bukkit.getWorld("arena_lobby");
        if (world == null) {
            plugin.getLogger().warning("World 'arena_lobby' not found. Leaderboard will not be created.");
            this.location = null;
            return;
        }
        this.location = new Location(world, -0.50, 68.00, 12.50, -87.75f, 2.18f);
        
        // Clean up existing leaderboard displays at this location
        cleanupOldDisplays();
        
        startUpdateTask();
    }

    private void cleanupOldDisplays() {
        if (location == null) return;
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1.0, 1.0, 1.0)) {
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("ffa_leaderboard")) {
                entity.remove();
            }
        }
    }

    private void startUpdateTask() {
        if (location == null) return;
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateLeaderboard, 20L, 1200L); // Every 60 seconds
    }

    public void updateLeaderboard() {
        if (location == null) return;
        plugin.getDatabaseManager().getTop10FFAKills(topKills -> {
            String content = buildLeaderboardText(topKills);
            
            if (leaderboardDisplay == null || !leaderboardDisplay.isValid()) {
                leaderboardDisplay = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
                leaderboardDisplay.addScoreboardTag("ffa_leaderboard");
                leaderboardDisplay.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            }
            
            leaderboardDisplay.setText(ChatUtils.formatNoPrefix(content));
        });
    }

    private String buildLeaderboardText(Map<String, Integer> topKills) {
        StringBuilder sb = new StringBuilder();
        sb.append("&6&l--- FFA Top 10 Killers ---\n");
        if (topKills.isEmpty()) {
            sb.append("&7No data yet\n");
        } else {
            int i = 1;
            for (Map.Entry<String, Integer> entry : topKills.entrySet()) {
                sb.append("&e").append(i++).append(". &f").append(entry.getKey()).append(": &a").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (leaderboardDisplay != null) {
            leaderboardDisplay.remove();
        }
    }
}
