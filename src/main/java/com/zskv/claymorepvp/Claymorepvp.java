package com.zskv.claymorepvp;

import com.zskv.claymorepvp.command.ClaymorepvpCommand;
import com.zskv.claymorepvp.command.DuelCommand;
import com.zskv.claymorepvp.database.DatabaseManager;
import com.zskv.claymorepvp.duel.DuelManager;
import com.zskv.claymorepvp.kit.KitManager;
import com.zskv.claymorepvp.leaderboard.LeaderboardManager;
import com.zskv.claymorepvp.listener.DuelListener;
import com.zskv.claymorepvp.listener.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Claymorepvp extends JavaPlugin {

    private DuelManager duelManager;
    private KitManager kitManager;
    private DatabaseManager databaseManager;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        this.kitManager = new KitManager(this);
        this.duelManager = new DuelManager(this, kitManager);
        this.leaderboardManager = new LeaderboardManager(this);

        getCommand("duel").setExecutor(new DuelCommand(duelManager, kitManager));
        getCommand("claymorepvp").setExecutor(new ClaymorepvpCommand(duelManager, kitManager));

        getServer().getPluginManager().registerEvents(new DuelListener(duelManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(duelManager), this);

        getLogger().info("ClaymorePvP has been enabled!");
    }

    @Override
    public void onDisable() {
        if (leaderboardManager != null) {
            leaderboardManager.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("ClaymorePvP has been disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
