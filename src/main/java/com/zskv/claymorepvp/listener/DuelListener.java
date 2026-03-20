package com.zskv.claymorepvp.listener;

import com.zskv.claymorepvp.duel.Duel;
import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.duel.DuelState;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DuelListener implements Listener {
    private final DuelManager duelManager;
    private final Set<UUID> fallDamageImmunity = new HashSet<>();
    private final Set<UUID> recentlyThrown = new HashSet<>();

    public DuelListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (duelManager.isPasting() && event.getBlock().getWorld().getName().equals("arena_maps")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (duelManager.isPasting() && event.getBlock().getWorld().getName().equals("arena_maps")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (duelManager.isPasting() && event.getBlock().getWorld().getName().equals("arena_maps")) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockGrow(BlockGrowEvent event) {
        if (duelManager.isPasting() && event.getBlock().getWorld().getName().equals("arena_maps")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockFade(BlockFadeEvent event) {
        if (duelManager.isPasting() && event.getBlock().getWorld().getName().equals("arena_maps")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (duelManager.isPasting() && event.getBlock().getWorld().getName().equals("arena_maps")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (fallDamageImmunity.remove(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        fallDamageImmunity.remove(victim.getUniqueId());
        if (duelManager.isInDuel(victim.getUniqueId())) {
            Player killer = victim.getKiller();
            if (killer != null) {
                duelManager.getPlugin().getDatabaseManager().incrementKills(killer.getUniqueId(), killer.getName());
                // Trigger leaderboard update after a short delay to allow DB update
                org.bukkit.Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> {
                    duelManager.getPlugin().getLeaderboardManager().updateLeaderboard();
                }, 20L);
            }
            event.setDeathMessage(null);
            event.getDrops().clear();
            Duel duel = duelManager.getDuel(victim.getUniqueId());
            UUID winnerUuid = duel.getOpponent(victim.getUniqueId());
            duelManager.endDuel(winnerUuid, victim.getUniqueId());
        } else {
            // FFA Tracking (Assume deaths in arena_lobby are FFA deaths)
            if (victim.getWorld().getName().equals("arena_lobby")) {
                Player killer = victim.getKiller();
                if (killer != null) {
                    duelManager.getPlugin().getDatabaseManager().incrementFFAKills(killer.getUniqueId(), killer.getName());
                    // Trigger leaderboard update after a short delay to allow DB update
                    org.bukkit.Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> {
                        duelManager.getPlugin().getLeaderboardManager().updateLeaderboard();
                    }, 20L);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        fallDamageImmunity.remove(player.getUniqueId());
        if (duelManager.isInDuel(player.getUniqueId())) {
            Duel duel = duelManager.getDuel(player.getUniqueId());
            UUID winnerUuid = duel.getOpponent(player.getUniqueId());
            duelManager.endDuel(winnerUuid, player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!duelManager.isInDuel(player.getUniqueId())) return;

        Duel duel = duelManager.getDuel(player.getUniqueId());
        // Enforce boundaries during STARTING (countdown) and ACTIVE states
        if (duel == null || (duel.getState() != DuelState.ACTIVE && duel.getState() != DuelState.STARTING) || duel.getMap() == null) return;

        Location base = duel.getBaseLocation();
        if (base == null) return;

        Location to = event.getTo();
        if (to == null) return;

        Vector min = duel.getMap().getFieldMin(base);
        Vector max = duel.getMap().getFieldMax(base);

        boolean outside = to.getX() < min.getX() || to.getX() > max.getX() ||
                          to.getY() < min.getY() || to.getY() > max.getY() ||
                          to.getZ() < min.getZ() || to.getZ() > max.getZ();

        if (outside) {
            // Prevent repeated triggers for a smoother experience
            if (recentlyThrown.contains(player.getUniqueId())) return;

            // Find direction to center
            Vector center = min.clone().add(max).multiply(0.5);
            Vector toVec = to.toVector();
            Vector direction = center.subtract(toVec).normalize();

            // Set a cooldown to prevent rapid firing
            recentlyThrown.add(player.getUniqueId());
            org.bukkit.Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> recentlyThrown.remove(player.getUniqueId()), 15L);

            // Use rotation-preserving reset to avoid "straightening out"
            Location bounceLoc = event.getFrom().clone();
            bounceLoc.setYaw(to.getYaw());
            bounceLoc.setPitch(to.getPitch());
            event.setTo(bounceLoc);
            
            // Apply horizontal and vertical force for the thrown UP and BACK effect
            // We use a slight delay for better physics processing on some servers
            org.bukkit.Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    // Horizontal force of 1.0 for a ~3 block bounce
                    Vector velocity = direction.clone().setY(0).normalize().multiply(1.0).setY(0.5);
                    player.setVelocity(velocity);
                }
            }, 1L);
            
            // Prevent fall damage from this specific throw
            fallDamageImmunity.add(player.getUniqueId());
        }
    }
}
