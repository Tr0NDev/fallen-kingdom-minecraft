package org.example.team;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.DyeColor;
import org.bukkit.Location;

public class TeamData {

    public final int id;
    public String name;
    public DyeColor color;
    public Location baseCenter;
    public int baseRadius;
    public final Set<UUID> members = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public TeamData(int id, String name) {
        this.id = id;
        this.name = name;
        this.color = DyeColor.WHITE;
        this.baseRadius = 0;
    }

}
