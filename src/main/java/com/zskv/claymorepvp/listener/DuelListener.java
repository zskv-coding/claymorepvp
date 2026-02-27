package com.zskv.claymorepvp.listener;

import com.zskv.claymorepvp.duel.Duel;
import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
            }
            event.setDeathMessage(null);
            event.getDrops().clear();
            Duel duel = duelManager.getDuel(victim.getUniqueId());
            UUID winnerUuid = duel.getOpponent(victim.getUniqueId());
            duelManager.endDuel(winnerUuid, victim.getUniqueId());
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

        Location to = event.getTo();
        if (to == null) return;

        // Boundaries: X: [-36, 40], Y: [100, 123], Z: [-39, 37]
        boolean outside = to.getX() < -36 || to.getX() > 40 ||
                          to.getY() < 100 || to.getY() > 123 ||
                          to.getZ() < -39 || to.getZ() > 37;

        if (outside) {
            // Prevent repeated triggers for a smoother experience
            if (recentlyThrown.contains(player.getUniqueId())) return;

            // Find direction to center
            Vector center = new Vector(2, 100, -1);
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
