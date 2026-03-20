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
        cleanupAllDisplays();
    }

    private void startUpdateTask() {
        if (location == null) return;
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateLeaderboard, 20L, 6000L); // Every 5 minutes
    }

    public void updateLeaderboard() {
        if (location == null) return;
        
        // Ensure the chunk is loaded so we can find/update the display
        if (!location.isChunkLoaded()) {
            location.getChunk().load();
        }

        plugin.getDatabaseManager().getTop10FFAKills(topKills -> {
            String content = buildLeaderboardText(topKills);
            
            // First check if our cached reference is still valid
            if (leaderboardDisplay != null && leaderboardDisplay.isValid() && leaderboardDisplay.getWorld().equals(location.getWorld())) {
                leaderboardDisplay.setText(ChatUtils.formatNoPrefix(content));
                // Periodically do a cleanup of any duplicates that might have leaked
                findAndCleanupDisplays();
                return;
            }
            
            // If not, search for an existing one or spawn a new one
            leaderboardDisplay = findAndCleanupDisplays();
            
            // If we still don't have one, spawn a new one
            if (leaderboardDisplay == null) {
                leaderboardDisplay = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
                leaderboardDisplay.addScoreboardTag("ffa_leaderboard");
                leaderboardDisplay.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                plugin.getLogger().info("Created new leaderboard TextDisplay at " + location.toString());
            }
            
            leaderboardDisplay.setText(ChatUtils.formatNoPrefix(content));
        });
    }

    private TextDisplay findAndCleanupDisplays() {
        if (location == null || location.getWorld() == null) return null;
        
        // Ensure chunk is loaded to be able to find entities
        if (!location.isChunkLoaded()) {
            location.getChunk().load();
        }

        TextDisplay found = null;
        
        // Use a slightly larger radius to find any duplicates
        for (Entity entity : location.getWorld().getNearbyEntities(location, 7.0, 7.0, 7.0)) {
            if (entity instanceof TextDisplay) {
                TextDisplay td = (TextDisplay) entity;
                
                // Check if it's our leaderboard by tag or content
                boolean isLeaderboard = td.getScoreboardTags().contains("ffa_leaderboard") || 
                                     (td.getText() != null && td.getText().contains("FFA Top 10 Killers"));
                
                if (isLeaderboard) {
                    if (found == null && td.isValid()) {
                        found = td;
                    } else {
                        // Remove any extra ones found
                        entity.remove();
                    }
                }
            }
        }
        return found;
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
        cleanupAllDisplays();
    }

    private void cleanupAllDisplays() {
        if (location == null || location.getWorld() == null) return;
        
        if (!location.isChunkLoaded()) {
            location.getChunk().load();
        }
        
        for (Entity entity : location.getWorld().getNearbyEntities(location, 10.0, 10.0, 10.0)) {
            if (entity instanceof TextDisplay) {
                TextDisplay td = (TextDisplay) entity;
                boolean isLeaderboard = td.getScoreboardTags().contains("ffa_leaderboard") || 
                                     (td.getText() != null && td.getText().contains("FFA Top 10 Killers"));
                if (isLeaderboard) {
                    entity.remove();
                }
            }
        }
    }
}
