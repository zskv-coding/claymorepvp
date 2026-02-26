package com.zskv.claymorepvp.duel;

import com.zskv.claymorepvp.Claymorepvp;
import com.zskv.claymorepvp.gui.DuelGUI;
import com.zskv.claymorepvp.kit.Kit;
import com.zskv.claymorepvp.kit.KitManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class DuelManager {
    private final Claymorepvp plugin;
    private final KitManager kitManager;
    private final Map<UUID, Duel> activeDuels = new HashMap<>();
    private final Map<UUID, Duel> pendingRequests = new HashMap<>();
    private Location spawn1;
    private Location spawn2;

    public DuelManager(Claymorepvp plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        initArena();
        loadLocations();
    }

    private void initArena() {
        World arenaWorld = Bukkit.getWorld("arena_maps");
        if (arenaWorld == null) {
            arenaWorld = Bukkit.createWorld(new WorldCreator("arena_maps"));
        }
        
        if (arenaWorld != null) {
            spawn1 = new Location(arenaWorld, 26.49, 100.00, -0.55, 89.93f, 0.30f);
            spawn2 = new Location(arenaWorld, -21.50, 100.00, -0.48, -89.72f, 0.87f);
            
            // Initial barrier setup
            setBarrier(true);
        }
    }

    private void setBarrier(boolean up) {
        World world = Bukkit.getWorld("arena_maps");
        if (world == null) return;

        Material material = up ? Material.BARRIER : Material.AIR;
        // Barrier = POS1: 2 100 -39 to POS2: 2 121 37
        for (int y = 100; y <= 121; y++) {
            for (int z = -39; z <= 37; z++) {
                world.getBlockAt(2, y, z).setType(material);
            }
        }
    }

    public void loadLocations() {
        if (plugin.getConfig().contains("arena.spawn1")) {
            spawn1 = plugin.getConfig().getLocation("arena.spawn1");
        }
        if (plugin.getConfig().contains("arena.spawn2")) {
            spawn2 = plugin.getConfig().getLocation("arena.spawn2");
        }
    }

    public void setSpawn1(Location loc) {
        this.spawn1 = loc;
        plugin.getConfig().set("arena.spawn1", loc);
        plugin.saveConfig();
    }

    public void setSpawn2(Location loc) {
        this.spawn2 = loc;
        plugin.getConfig().set("arena.spawn2", loc);
        plugin.saveConfig();
    }

    public boolean canStartDuel() {
        return spawn1 != null && spawn2 != null;
    }

    public void sendRequest(Player challenger, Player challenged, String kitName) {
        if (isInDuel(challenger.getUniqueId()) || isInDuel(challenged.getUniqueId())) {
            challenger.sendMessage(ChatUtils.format("&cOne of the players is already in a duel!"));
            return;
        }

        Duel duel = new Duel(challenger.getUniqueId(), challenged.getUniqueId(), kitName);
        pendingRequests.put(challenged.getUniqueId(), duel);
        
        challenger.sendMessage(ChatUtils.format("&aDuel request sent to &e" + challenged.getName() + (kitName != null ? "&a with kit &e" + kitName : "")));
        challenged.sendMessage(ChatUtils.format("&e" + challenger.getName() + " &ahas challenged you to a duel!" + (kitName != null ? " &7(Kit: &e" + kitName + "&7)" : "")));
        challenged.openInventory(DuelGUI.createRequestGUI(challenger));
        
        // Timeout request after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.get(challenged.getUniqueId()) == duel) {
                pendingRequests.remove(challenged.getUniqueId());
                challenger.sendMessage(ChatUtils.format("&cDuel request to &e" + challenged.getName() + " &cexpired."));
                challenged.sendMessage(ChatUtils.format("&cDuel request from &e" + challenger.getName() + " &cexpired."));
            }
        }, 600L);
    }

    public void acceptRequest(Player challenged, Player challenger) {
        Duel duel = pendingRequests.get(challenged.getUniqueId());
        if (duel == null || !duel.getChallenger().equals(challenger.getUniqueId())) {
            challenged.sendMessage(ChatUtils.format("&cNo pending duel request from &e" + challenger.getName()));
            return;
        }

        if (!canStartDuel()) {
            challenged.sendMessage(ChatUtils.format("&cArena is not set up! Contact an administrator."));
            return;
        }

        pendingRequests.remove(challenged.getUniqueId());
        startDuel(duel);
    }

    private void startDuel(Duel duel) {
        Player p1 = Bukkit.getPlayer(duel.getChallenger());
        Player p2 = Bukkit.getPlayer(duel.getChallenged());

        if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
            return;
        }

        duel.setState(DuelState.STARTING);
        activeDuels.put(p1.getUniqueId(), duel);
        activeDuels.put(p2.getUniqueId(), duel);

        p1.teleport(spawn1);
        p2.teleport(spawn2);

        p1.sendMessage(ChatUtils.format("&aDuel starting in &e10 &aseconds..."));
        p2.sendMessage(ChatUtils.format("&aDuel starting in &e10 &aseconds..."));

        // Ensure barrier is up
        setBarrier(true);

        // Countdown
        new org.bukkit.scheduler.BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    p1.sendMessage(ChatUtils.formatNoPrefix("&6&lFIGHT!"));
                    p2.sendMessage(ChatUtils.formatNoPrefix("&6&lFIGHT!"));
                    setBarrier(false);
                    duel.setState(DuelState.ACTIVE);

                    if (duel.getKitName() != null) {
                        Kit kit = kitManager.getKit(duel.getKitName());
                        if (kit != null) {
                            kit.apply(p1);
                            kit.apply(p2);
                        }
                    }

                    this.cancel();
                    return;
                }
                
                if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
                    endDuel(p1 != null ? p1.getUniqueId() : null, p2 != null ? p2.getUniqueId() : null);
                    this.cancel();
                    return;
                }

                p1.sendMessage(ChatUtils.formatNoPrefix("&e" + countdown + "..."));
                p2.sendMessage(ChatUtils.formatNoPrefix("&e" + countdown + "..."));
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void endDuel(UUID winnerUuid, UUID loserUuid) {
        Duel duel = null;
        if (winnerUuid != null) duel = activeDuels.remove(winnerUuid);
        if (loserUuid != null) activeDuels.remove(loserUuid);

        if (duel != null) {
            duel.setState(DuelState.FINISHED);
            Player winner = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;
            Player loser = loserUuid != null ? Bukkit.getPlayer(loserUuid) : null;

            if (winner != null) winner.sendMessage(ChatUtils.format("&aYou won the duel!"));
            if (loser != null) loser.sendMessage(ChatUtils.format("&cYou lost the duel!"));
            
            if (winner != null) winner.teleport(winner.getWorld().getSpawnLocation());
            if (loser != null) loser.teleport(loser.getWorld().getSpawnLocation());
            
            // Reset barrier if no other duels are active (for now only 1 duel at a time :>)
            if (activeDuels.isEmpty()) {
                setBarrier(true);
            }
        }
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public Duel getDuel(UUID uuid) {
        return activeDuels.get(uuid);
    }
}
