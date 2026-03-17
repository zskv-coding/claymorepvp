package com.zskv.claymorepvp.duel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class DuelMap {
    private final String name;
    private final String structureName;
    private Vector pasteOffset = new Vector(0, 0, 0);
    private Vector spawn1 = null;
    private Vector spawn2 = null;
    private Vector barrierMin = null;
    private Vector barrierMax = null;
    private Vector fieldMin = null;
    private Vector fieldMax = null;

    public DuelMap(String name, String structureName) {
        this.name = name;
        this.structureName = structureName;
    }

    public String getName() {
        return name;
    }

    public String getStructureName() {
        return structureName;
    }

    public Vector getPasteOffset() {
        return pasteOffset;
    }

    public void setPasteOffset(Vector pasteOffset) {
        this.pasteOffset = pasteOffset;
    }

    public void setSpawn1(Vector spawn1) { this.spawn1 = spawn1; }
    public void setSpawn2(Vector spawn2) { this.spawn2 = spawn2; }
    public void setBarrierMin(Vector barrierMin) { this.barrierMin = barrierMin; }
    public void setBarrierMax(Vector barrierMax) { this.barrierMax = barrierMax; }
    public void setFieldMin(Vector fieldMin) { this.fieldMin = fieldMin; }
    public void setFieldMax(Vector fieldMax) { this.fieldMax = fieldMax; }

    public boolean isLoaded() {
        return spawn1 != null && spawn2 != null && barrierMin != null && barrierMax != null && fieldMin != null && fieldMax != null;
    }

    public Location getSpawn1(Location base) {
        return base.clone().add(spawn1).add(0.5, 1.2, 0.5); // Slightly above block to prevent floor clipping
    }

    public Location getSpawn2(Location base) {
        return base.clone().add(spawn2).add(0.5, 1.2, 0.5); // Slightly above block to prevent floor clipping
    }

    public Vector getBarrierMin(Location base) {
        return base.toVector().add(Vector.getMinimum(barrierMin, barrierMax));
    }

    public Vector getBarrierMax(Location base) {
        return base.toVector().add(Vector.getMaximum(barrierMin, barrierMax));
    }

    public Vector getFieldMin(Location base) {
        return base.toVector().add(Vector.getMinimum(fieldMin, fieldMax));
    }

    public Vector getFieldMax(Location base) {
        return base.toVector().add(Vector.getMaximum(fieldMin, fieldMax));
    }
}
