package com.zskv.claymorepvp.duel;

import org.bukkit.entity.Player;
import java.util.UUID;

public class Duel {
    private final UUID challenger;
    private final UUID challenged;
    private DuelState state;
    private long requestTime;
    private String kitName;
    private DuelMap map;
    private org.bukkit.Location baseLocation;
    private boolean accepted = false;

    public Duel(UUID challenger, UUID challenged, String kitName) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.kitName = kitName;
        this.state = DuelState.REQUESTED;
        this.requestTime = System.currentTimeMillis();
    }

    public String getKitName() {
        return kitName;
    }

    public UUID getChallenger() {
        return challenger;
    }

    public UUID getChallenged() {
        return challenged;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public DuelMap getMap() {
        return map;
    }

    public void setMap(DuelMap map) {
        this.map = map;
    }

    public org.bukkit.Location getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(org.bukkit.Location baseLocation) {
        this.baseLocation = baseLocation;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public boolean isParticipant(UUID uuid) {
        return challenger.equals(uuid) || challenged.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (challenger.equals(uuid)) return challenged;
        if (challenged.equals(uuid)) return challenger;
        return null;
    }
}
