package org.example.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsManager {

    public static class PlayerStats {
        private int kills;
        private int deaths;
        private int oresMined;
        private int animalsKilled;
        private int basesCaptured;
        private int gamesWon;

        public PlayerStats() {
        }

        public PlayerStats(PlayerStats other) {
            if (other == null) {
                return;
            }
            this.kills = other.kills;
            this.deaths = other.deaths;
            this.oresMined = other.oresMined;
            this.animalsKilled = other.animalsKilled;
            this.basesCaptured = other.basesCaptured;
            this.gamesWon = other.gamesWon;
        }

        public int getKills() {
            return kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public int getOresMined() {
            return oresMined;
        }

        public int getAnimalsKilled() {
            return animalsKilled;
        }

        public int getBasesCaptured() {
            return basesCaptured;
        }

        public int getGamesWon() {
            return gamesWon;
        }

        public void setKills(int kills) {
            this.kills = Math.max(0, kills);
        }

        public void setDeaths(int deaths) {
            this.deaths = Math.max(0, deaths);
        }

        public void setOresMined(int oresMined) {
            this.oresMined = Math.max(0, oresMined);
        }

        public void setAnimalsKilled(int animalsKilled) {
            this.animalsKilled = Math.max(0, animalsKilled);
        }

        public void setBasesCaptured(int basesCaptured) {
            this.basesCaptured = Math.max(0, basesCaptured);
        }

        public void setGamesWon(int gamesWon) {
            this.gamesWon = Math.max(0, gamesWon);
        }

        public void addKill() {
            this.kills++;
        }

        public void addDeath() {
            this.deaths++;
        }

        public void addOreMined() {
            this.oresMined++;
        }

        public void addAnimalKilled() {
            this.animalsKilled++;
        }

        public void addBaseCaptured() {
            this.basesCaptured++;
        }

        public void addGameWon() {
            this.gamesWon++;
        }
    }

    private final Map<UUID, PlayerStats> matchStats = new ConcurrentHashMap<UUID, PlayerStats>();
    private final Map<UUID, PlayerStats> globalStats = new ConcurrentHashMap<UUID, PlayerStats>();
    private Runnable saveAction = new Runnable() {
        @Override
        public void run() {
        }
    };

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction == null ? new Runnable() {
            @Override
            public void run() {
            }
        } : saveAction;
    }

    public PlayerStats getPlayerStats(UUID playerId) {
        PlayerStats stats = matchStats.get(playerId);
        return stats == null ? new PlayerStats() : new PlayerStats(stats);
    }

    public PlayerStats getGlobalPlayerStats(UUID playerId) {
        PlayerStats stats = globalStats.get(playerId);
        return stats == null ? new PlayerStats() : new PlayerStats(stats);
    }

    public Map<UUID, PlayerStats> getSnapshot() {
        Map<UUID, PlayerStats> snapshot = new HashMap<UUID, PlayerStats>();
        for (Map.Entry<UUID, PlayerStats> entry : matchStats.entrySet()) {
            snapshot.put(entry.getKey(), new PlayerStats(entry.getValue()));
        }
        return snapshot;
    }

    public Map<UUID, PlayerStats> getGlobalSnapshot() {
        Map<UUID, PlayerStats> snapshot = new HashMap<UUID, PlayerStats>();
        for (Map.Entry<UUID, PlayerStats> entry : globalStats.entrySet()) {
            snapshot.put(entry.getKey(), new PlayerStats(entry.getValue()));
        }
        return snapshot;
    }

    public void setAll(Map<UUID, PlayerStats> loadedStats) {
        matchStats.clear();
        if (loadedStats == null) {
            return;
        }

        for (Map.Entry<UUID, PlayerStats> entry : loadedStats.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            matchStats.put(entry.getKey(), new PlayerStats(entry.getValue()));
        }
    }

    public void setAllGlobal(Map<UUID, PlayerStats> loadedStats) {
        globalStats.clear();
        if (loadedStats == null) {
            return;
        }

        for (Map.Entry<UUID, PlayerStats> entry : loadedStats.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            globalStats.put(entry.getKey(), new PlayerStats(entry.getValue()));
        }
    }

    public void resetAll() {
        matchStats.clear();
        saveAction.run();
    }

    public void addKill(UUID playerId) {
        getMutable(matchStats, playerId).addKill();
        getMutable(globalStats, playerId).addKill();
        saveAction.run();
    }

    public void addDeath(UUID playerId) {
        getMutable(matchStats, playerId).addDeath();
        getMutable(globalStats, playerId).addDeath();
        saveAction.run();
    }

    public void addOreMined(UUID playerId) {
        getMutable(matchStats, playerId).addOreMined();
        getMutable(globalStats, playerId).addOreMined();
        saveAction.run();
    }

    public void addAnimalKilled(UUID playerId) {
        getMutable(matchStats, playerId).addAnimalKilled();
        getMutable(globalStats, playerId).addAnimalKilled();
        saveAction.run();
    }

    public void addBaseCaptured(UUID playerId) {
        getMutable(matchStats, playerId).addBaseCaptured();
        getMutable(globalStats, playerId).addBaseCaptured();
        saveAction.run();
    }

    public void addGameWon(UUID playerId) {
        getMutable(matchStats, playerId).addGameWon();
        getMutable(globalStats, playerId).addGameWon();
        saveAction.run();
    }

    private PlayerStats getMutable(Map<UUID, PlayerStats> source, UUID playerId) {
        PlayerStats current = source.get(playerId);
        if (current == null) {
            current = new PlayerStats();
            source.put(playerId, current);
        }
        return current;
    }
}
