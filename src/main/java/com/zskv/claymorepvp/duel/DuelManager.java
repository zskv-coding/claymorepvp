package com.zskv.claymorepvp.duel;

import com.zskv.claymorepvp.Claymorepvp;
import com.zskv.claymorepvp.gui.DuelGUI;
import com.zskv.claymorepvp.kit.Kit;
import com.zskv.claymorepvp.kit.KitManager;
import com.zskv.claymorepvp.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DuelManager {
    private final Claymorepvp plugin;
    private final KitManager kitManager;
    private final Map<UUID, Duel> activeDuels = new HashMap<>();
    private final Map<UUID, Duel> pendingRequests = new HashMap<>();
    private final Map<String, DuelMap> maps = new HashMap<>();
    private int nextDuelOffset = 0;
    private boolean isPasting = false;
    private Location spawn1;
    private Location spawn2;

    public DuelManager(Claymorepvp plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        initArena();
        loadLocations();
        loadMaps();
    }

    public Claymorepvp getPlugin() {
        return plugin;
    }

    public boolean isPasting() {
        return isPasting;
    }

    private void initArena() {
        World arenaWorld = Bukkit.getWorld("arena_maps");
        if (arenaWorld == null) {
            arenaWorld = Bukkit.createWorld(new WorldCreator("arena_maps"));
        }
    }

    private void setBarrier(Duel duel, boolean up) {
        World world = Bukkit.getWorld("arena_maps");
        DuelMap map = duel.getMap();
        Location base = duel.getBaseLocation();
        if (world == null || map == null || base == null) return;

        Material material = up ? Material.BARRIER : Material.AIR;
        Vector min = map.getBarrierMin(base);
        Vector max = map.getBarrierMax(base);

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    world.getBlockAt(x, y, z).setType(material);
                }
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

    private double getCoord(ConfigurationSection section, String path) {
        Object val = section.get(path);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0.0;
    }

    private void loadMaps() {
        maps.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("maps");
        if (section == null) {
            plugin.getLogger().warning("No 'maps' section found in config.yml!");
            return;
        }

        File structuresDir = new File(plugin.getDataFolder(), "structures");
        if (!structuresDir.exists()) structuresDir.mkdirs();

        World arenaWorld = Bukkit.getWorld("arena_maps");
        if (arenaWorld == null) {
            arenaWorld = Bukkit.createWorld(new WorldCreator("arena_maps"));
        }

        int scanX = -2000;

        for (String key : section.getKeys(false)) {
            String structureName = section.getString(key + ".structure");
            if (structureName == null) continue;

            File structureFile = new File(structuresDir, structureName);
            if (!structureFile.exists()) {
                plugin.getLogger().warning("Structure file " + structureName + " for map " + key + " not found!");
                continue;
            }

            DuelMap map = new DuelMap(key, structureName);

            // Load paste offset if any
            Vector pOffset = section.getVector(key + ".paste-offset");
            if (pOffset == null && section.contains(key + ".paste-offset"))
                pOffset = new Vector(getCoord(section, key + ".paste-offset.x"), getCoord(section, key + ".paste-offset.y"), getCoord(section, key + ".paste-offset.z"));
            if (pOffset != null) map.setPasteOffset(pOffset);

            // PRE-SCAN markers
            Location scanLoc = new Location(arenaWorld, scanX, 100, 0);
            
            // Just scan the structure file directly without pasting/clearing in the world
            if (scanStructureFromFile(map)) {
                maps.put(key, map);
                plugin.getLogger().info("Loaded duel map: " + key + " (Scanned from file)");
            } else {
                plugin.getLogger().warning("Failed to load map " + key + "! Markers missing in structure.");
            }
            scanX -= 500;
        }
    }

    private boolean scanStructureFromFile(DuelMap map) {
        StructureManager manager = Bukkit.getStructureManager();
        try {
            File structuresDir = new File(plugin.getDataFolder(), "structures");
            File structureFile = new File(structuresDir, map.getStructureName());
            Structure structure = manager.loadStructure(structureFile);
            
            // We need to use structure.getPalettes() or similar if available, 
            // but the easiest way without pasting is to use structure.getEntities() and blocks if possible.
            // However, Bukkit's Structure API is limited. 
            // If we MUST paste to scan, we should do it once and leave it, or use a separate world.
            // Since we already have the scan logic, let's just make it only happen if not already loaded.
            
            World arenaWorld = Bukkit.getWorld("arena_maps");
            Location scanLoc = new Location(arenaWorld, -5000, 100, 0); // Far away
            
            pasteMap(map, scanLoc);
            scanStructure(scanLoc, map);
            clearArea(arenaWorld, map, scanLoc);
            
            return map.isLoaded();
        } catch (IOException e) {
            return false;
        }
    }

    private void scanStructure(Location pasteLoc, DuelMap map) {
        World world = pasteLoc.getWorld();
        if (world == null || map == null) return;
        StructureManager manager = Bukkit.getStructureManager();
        try {
            File structuresDir = new File(plugin.getDataFolder(), "structures");
            File structureFile = new File(structuresDir, map.getStructureName());
            Structure structure = manager.loadStructure(structureFile);
            Vector size = structure.getSize();
            for (int x = 0; x < size.getBlockX(); x++) {
                for (int y = 0; y < size.getBlockY(); y++) {
                    for (int z = 0; z < size.getBlockZ(); z++) {
                        Location loc = pasteLoc.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);
                        Material mat = block.getType();
                        if (mat == Material.AIR) continue;

                        Vector relPos = new Vector(x + map.getPasteOffset().getX(), y + map.getPasteOffset().getY(), z + map.getPasteOffset().getZ());
                        if (mat == Material.LIME_WOOL) map.setSpawn1(relPos);
                        else if (mat == Material.ORANGE_WOOL) map.setSpawn2(relPos);
                        else if (mat == Material.RED_WOOL) map.setBarrierMin(relPos);
                        else if (mat == Material.BLUE_WOOL) map.setBarrierMax(relPos);
                        else if (mat == Material.BLACK_WOOL) map.setFieldMin(relPos);
                        else if (mat == Material.WHITE_WOOL) map.setFieldMax(relPos);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void clearArea(World world, DuelMap map, Location base) {
        StructureManager manager = Bukkit.getStructureManager();
        try {
            File structuresDir = new File(plugin.getDataFolder(), "structures");
            File structureFile = new File(structuresDir, map.getStructureName());
            Structure structure = manager.loadStructure(structureFile);
            Vector size = structure.getSize();
            for (int x = 0; x < size.getBlockX(); x++) {
                for (int y = 0; y < size.getBlockY(); y++) {
                    for (int z = 0; z < size.getBlockZ(); z++) {
                        Block block = world.getBlockAt(base.clone().add(x, y, z));
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR, false); // false = no physics
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void cleanupMarkers(Duel duel, Location pasteLoc) {
        DuelMap map = duel.getMap();
        World world = pasteLoc.getWorld();
        if (world == null || map == null) return;
        StructureManager manager = Bukkit.getStructureManager();
        isPasting = true;
        try {
            File structuresDir = new File(plugin.getDataFolder(), "structures");
            File structureFile = new File(structuresDir, map.getStructureName());
            Structure structure = manager.loadStructure(structureFile);
            Vector size = structure.getSize();
            for (int x = 0; x < size.getBlockX(); x++) {
                for (int y = 0; y < size.getBlockY(); y++) {
                    for (int z = 0; z < size.getBlockZ(); z++) {
                        Location loc = pasteLoc.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);
                        Material mat = block.getType();
                        if (mat == Material.LIME_WOOL || mat == Material.ORANGE_WOOL) {
                            block.setType(Material.BEACON, false);
                        } else if (mat == Material.RED_WOOL || mat == Material.BLUE_WOOL || mat == Material.BLACK_WOOL || mat == Material.WHITE_WOOL) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            isPasting = false;
        }
    }

    private void pasteMap(DuelMap map, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        File structuresDir = new File(plugin.getDataFolder(), "structures");
        if (!structuresDir.exists()) structuresDir.mkdirs();

        File structureFile = new File(structuresDir, map.getStructureName());
        if (!structureFile.exists()) {
            plugin.getLogger().warning("Structure file " + map.getStructureName() + " not found!");
            return;
        }

        StructureManager manager = Bukkit.getStructureManager();
        isPasting = true;
        try {
            Structure structure = manager.loadStructure(structureFile);
            structure.place(loc, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            isPasting = false;
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

    public void reload() {
        plugin.reloadConfig();
        kitManager.loadKits();
        loadLocations();
        loadMaps();
    }

    public boolean canStartDuel() {
        return !maps.isEmpty();
    }

    private DuelMap getRandomMap() {
        if (maps.isEmpty()) return null;
        List<DuelMap> list = new ArrayList<>(maps.values());
        return list.get(new Random().nextInt(list.size()));
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

        if (duel.isAccepted()) return; // Already being processed

        if (!canStartDuel()) {
            challenged.sendMessage(ChatUtils.format("&cArena is not set up! Contact an administrator."));
            return;
        }

        duel.setAccepted(true);
        pendingRequests.remove(challenged.getUniqueId());
        startDuel(duel);
    }

    private void startDuel(Duel duel) {
        Player p1 = Bukkit.getPlayer(duel.getChallenger());
        Player p2 = Bukkit.getPlayer(duel.getChallenged());

        if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
            return;
        }

        DuelMap map = getRandomMap();
        if (map == null) {
            p1.sendMessage(ChatUtils.format("&cArena is not set up! Check config.yml."));
            p2.sendMessage(ChatUtils.format("&cArena is not set up! Check config.yml."));
            return;
        }

        World arenaWorld = Bukkit.getWorld("arena_maps");
        if (arenaWorld == null) {
            arenaWorld = Bukkit.createWorld(new WorldCreator("arena_maps"));
        }

        Location baseLoc = new Location(arenaWorld, nextDuelOffset, 100, 0);
        nextDuelOffset += 500; // Spread out by 500 blocks each time

        duel.setMap(map);
        duel.setBaseLocation(baseLoc);
        Location pasteLoc = baseLoc.clone().add(map.getPasteOffset());
        pasteMap(map, pasteLoc);
        cleanupMarkers(duel, pasteLoc);

        // Increment matches and kit plays
        plugin.getDatabaseManager().incrementMatches(p1.getUniqueId(), p1.getName());
        plugin.getDatabaseManager().incrementMatches(p2.getUniqueId(), p2.getName());

        if (duel.getKitName() != null) {
            plugin.getDatabaseManager().incrementKitPlays(p1.getUniqueId(), duel.getKitName());
            plugin.getDatabaseManager().incrementKitPlays(p2.getUniqueId(), duel.getKitName());
        }

        duel.setState(DuelState.STARTING);
        activeDuels.put(p1.getUniqueId(), duel);
        activeDuels.put(p2.getUniqueId(), duel);

        // Teleport after a longer delay to ensure client has loaded the map area and physics has settled
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p1 != null && p1.isOnline() && p2 != null && p2.isOnline()) {
                p1.setFallDistance(0);
                p2.setFallDistance(0);
                p1.setVelocity(new Vector(0, 0, 0));
                p2.setVelocity(new Vector(0, 0, 0));
                p1.teleport(map.getSpawn1(baseLoc));
                p2.teleport(map.getSpawn2(baseLoc));
            }
        }, 30L);

        p1.setGameMode(GameMode.ADVENTURE);
        p2.setGameMode(GameMode.ADVENTURE);

        if (duel.getKitName() != null) {
            Kit kit = kitManager.getKit(duel.getKitName());
            if (kit != null) {
                kit.apply(p1);
                kit.apply(p2);
            }
        }

        // Ensure barrier is up
        setBarrier(duel, true);

        // Countdown
        new org.bukkit.scheduler.BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    String fightMsg = ChatUtils.formatNoPrefix("&6&lFIGHT!");
                    p1.sendTitle(fightMsg, "", 5, 20, 5);
                    p2.sendTitle(fightMsg, "", 5, 20, 5);
                    
                    p1.playSound(p1.getLocation(), "custom:duels_countdown_go", SoundCategory.VOICE, 1.0f, 1.0f);
                    p2.playSound(p2.getLocation(), "custom:duels_countdown_go", SoundCategory.VOICE, 1.0f, 1.0f);

                    // Spawn fireworks at specific locations - relative to baseLocation
                    Location base = duel.getBaseLocation();
                    spawnFirework(base.clone().add(31, 0.5, -1), Color.RED, p1, p2);
                    spawnFirework(base.clone().add(-27, 0.5, -1), Color.BLUE, p1, p2);

                    p1.sendMessage(fightMsg);
                    p2.sendMessage(fightMsg);
                    
                    setBarrier(duel, false);
                    duel.setState(DuelState.ACTIVE);

                    this.cancel();
                    return;
                }
                
                if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
                    endDuel(p1 != null ? p1.getUniqueId() : null, p2 != null ? p2.getUniqueId() : null);
                    this.cancel();
                    return;
                }

                String title = ChatUtils.formatNoPrefix("&aGame starting in:");
                String color;
                if (countdown == 3) color = "&e"; // Yellow
                else if (countdown == 2) color = "&6"; // Orange
                else if (countdown == 1) color = "&c"; // Red
                else color = "&a"; // Green for 4-10
                
                String subtitle = ChatUtils.formatNoPrefix(color + countdown);
                
                p1.sendTitle(title, subtitle, 0, 21, 0);
                p2.sendTitle(title, subtitle, 0, 21, 0);

                // Action Bar message: [ Challenger vs Challenged ] - Kit: KitName
                String kitSuffix = duel.getKitName() != null ? " &7- Kit: &e" + duel.getKitName() : "";
                String actionBarMsg = ChatUtils.formatNoPrefix("&e" + p1.getName() + " &7vs &e" + p2.getName() + kitSuffix);
                p1.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionBarMsg));
                p2.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionBarMsg));
                
                String sound;
                if (countdown <= 3 && countdown >= 1) {
                    sound = "custom:duels_countdown_" + countdown;
                } else {
                    sound = "custom:duels_countdown_default";
                }
                
                p1.playSound(p1.getLocation(), sound, SoundCategory.VOICE, 1.0f, 1.0f);
                p2.playSound(p2.getLocation(), sound, SoundCategory.VOICE, 1.0f, 1.0f);

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

            if (winner != null) {
                winner.sendMessage(ChatUtils.format("&aYou won the duel!"));
                winner.getInventory().clear();
                winner.getInventory().setArmorContents(null);
            }
            if (loser != null) {
                loser.sendMessage(ChatUtils.format("&cYou lost the duel!"));
                loser.getInventory().clear();
                loser.getInventory().setArmorContents(null);
            }
            
            // Clear items on floor in arena_maps
            World arenaWorld = Bukkit.getWorld("arena_maps");
            if (arenaWorld != null) {
                // Clear items only in the relevant duel area
                Location base = duel.getBaseLocation();
                DuelMap map = duel.getMap();
                if (base != null && map != null) {
                    Vector fMin = map.getFieldMin(base);
                    Vector fMax = map.getFieldMax(base);
                    
                    arenaWorld.getEntitiesByClass(Item.class).forEach(item -> {
                        Location loc = item.getLocation();
                        if (loc.getX() >= fMin.getX() && loc.getX() <= fMax.getX() &&
                            loc.getZ() >= fMin.getZ() && loc.getZ() <= fMax.getZ()) {
                            item.remove();
                        }
                    });

                    // Reset barrier for THIS duel
                    setBarrier(duel, false);
                    
                    // Clear the entire map structure area
                    clearArea(arenaWorld, map, base.clone().add(map.getPasteOffset()));
                }
            }
            
            // Teleport all players to hub
            World hubWorld = Bukkit.getWorld("Arena_Lobby");
            if (hubWorld == null) {
                hubWorld = Bukkit.createWorld(new WorldCreator("Arena_Lobby"));
            }
            
            if (hubWorld != null) {
                Location hubLoc = new Location(hubWorld, 9.57, 67, 17.47, 90f, 0f);
                
                // Use a small delay for teleporting to hub in case players are dead and need to respawn
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (winner != null && winner.isOnline()) {
                        if (winner.isDead()) winner.spigot().respawn();
                        winner.teleport(hubLoc);
                    }
                    if (loser != null && loser.isOnline()) {
                        if (loser.isDead()) loser.spigot().respawn();
                        loser.teleport(hubLoc);
                    }
                }, 5L);
            }
        }
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public Duel getDuel(UUID uuid) {
        return activeDuels.get(uuid);
    }

    private void spawnFirework(Location loc, Color color, Player... viewers) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder()
                .withColor(color)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        fwm.setPower(2); // Flies up before exploding
        fw.setFireworkMeta(fwm);

        // Hide from everyone else
        List<Player> viewerList = Arrays.asList(viewers);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!viewerList.contains(p)) {
                p.hideEntity(plugin, fw);
            }
        }
    }
}
