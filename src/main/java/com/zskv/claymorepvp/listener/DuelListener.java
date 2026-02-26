package com.zskv.claymorepvp.listener;

import com.zskv.claymorepvp.duel.Duel;
import com.zskv.claymorepvp.duel.DuelManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.UUID;

public class DuelListener implements Listener {
    private final DuelManager duelManager;

    public DuelListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (duelManager.isInDuel(victim.getUniqueId())) {
            Duel duel = duelManager.getDuel(victim.getUniqueId());
            UUID winnerUuid = duel.getOpponent(victim.getUniqueId());
            duelManager.endDuel(winnerUuid, victim.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
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
            // Bounce back: teleport to 'from' and set velocity away from the boundary
            event.setTo(event.getFrom());
            player.sendMessage("You cannot leave the arena!");
            
            // Optional: apply a slight knockback towards the center
            Vector center = new Vector(2, 100, -1); // Roughly the center of the arena
            Vector direction = center.subtract(to.toVector()).normalize().multiply(0.5);
            player.setVelocity(direction);
        }
    }
}
