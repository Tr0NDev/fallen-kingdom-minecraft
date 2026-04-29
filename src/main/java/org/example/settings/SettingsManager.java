package org.example.settings;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SettingsManager {

    private boolean baseMessagesEnabled;
    private boolean chestRoomMessagesEnabled;
    private int netherCapDay;
    private int endCapDay;
    private int pvpCapDay;
    private int tntCapDay;
    private boolean friendlyFireEnabled;
    private boolean healthBelowNameEnabled;
    private boolean dayHudEnabled;
    private boolean nightVisionEnabled;
    private int cavePlaceYLimit;
    private boolean enderPearlEnabled;
    private boolean chargedCreeperDropTntEnabled;
    private int chargedCreeperSpawnChance;
    private boolean tntClimbEnabled;
    private boolean compassEnabled;
    private boolean autoPauseOnQuitEnabled;
    private boolean bucketAssaultEnabled;
    private boolean netherAssaultEnabled;
    private boolean potionsEnabled;
    private boolean coloredNamesEnabled;
    private int baseCaptureTimeSeconds;
    private int baseCaptureRequiredPlayers;
    private double hungerConsumptionRate;
    private Integer deathLimit;
    private final List<ItemStack> baseLoadoutItems = new ArrayList<ItemStack>();
    private final Set<String> globallyAllowedPlaceBlocks = new HashSet<String>();
    private int currentDay;
    private Runnable saveAction = new Runnable() {
        @Override
        public void run() {
        }
    };

    public SettingsManager() {
        this.currentDay = 0;
        this.chestRoomMessagesEnabled = true;
        this.netherCapDay = 0;
        this.endCapDay = 0;
        this.pvpCapDay = 0;
        this.tntCapDay = 0;
        this.friendlyFireEnabled = false;
        this.healthBelowNameEnabled = false;
        this.dayHudEnabled = true;
        this.nightVisionEnabled = false;
        this.cavePlaceYLimit = 50;
        this.enderPearlEnabled = true;
        this.chargedCreeperDropTntEnabled = false;
        this.chargedCreeperSpawnChance = 0;
        this.tntClimbEnabled = true;
        this.compassEnabled = false;
        this.autoPauseOnQuitEnabled = false;
        this.bucketAssaultEnabled = true;
        this.netherAssaultEnabled = true;
        this.potionsEnabled = true;
        this.coloredNamesEnabled = true;
        this.baseCaptureTimeSeconds = 30;
        this.baseCaptureRequiredPlayers = 1;
        this.hungerConsumptionRate = 1.0D;
        this.deathLimit = null;
    }

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction == null ? new Runnable() {
            @Override
            public void run() {
            }
        } : saveAction;
    }

    public boolean isBaseMessagesEnabled() {
        return baseMessagesEnabled;
    }

    public void setBaseMessagesEnabled(boolean baseMessagesEnabled) {
        this.baseMessagesEnabled = baseMessagesEnabled;
    }

    public void toggleBaseMessages() {
        this.baseMessagesEnabled = !this.baseMessagesEnabled;
        saveAction.run();
    }

    public boolean isChestRoomMessagesEnabled() {
        return chestRoomMessagesEnabled;
    }

    public void setChestRoomMessagesEnabled(boolean chestRoomMessagesEnabled) {
        this.chestRoomMessagesEnabled = chestRoomMessagesEnabled;
        saveAction.run();
    }

    public void toggleChestRoomMessages() {
        this.chestRoomMessagesEnabled = !this.chestRoomMessagesEnabled;
        saveAction.run();
    }

    public boolean isBaseMessagesSelfOnly() {
        return chestRoomMessagesEnabled;
    }

    public void setBaseMessagesSelfOnly(boolean baseMessagesSelfOnly) {
        setChestRoomMessagesEnabled(baseMessagesSelfOnly);
    }

    public void toggleBaseMessagesSelfOnly() {
        toggleChestRoomMessages();
    }

    public boolean isNetherCapEnabled() {
        return isEnabledAtDay(netherCapDay);
    }

    public int getNetherCapDay() {
        return netherCapDay;
    }

    public void setNetherCapDay(int netherCapDay) {
        this.netherCapDay = normalizeActivationDay(netherCapDay);
        saveAction.run();
    }

    public boolean isEndCapEnabled() {
        return isEnabledAtDay(endCapDay);
    }

    public int getEndCapDay() {
        return endCapDay;
    }

    public void setEndCapDay(int endCapDay) {
        this.endCapDay = normalizeActivationDay(endCapDay);
        saveAction.run();
    }

    public boolean isPvpCapEnabled() {
        return isEnabledAtDay(pvpCapDay);
    }

    public int getPvpCapDay() {
        return pvpCapDay;
    }

    public void setPvpCapDay(int pvpCapDay) {
        this.pvpCapDay = normalizeActivationDay(pvpCapDay);
        saveAction.run();
    }

    public boolean isTntCapEnabled() {
        return isEnabledAtDay(tntCapDay);
    }

    public int getTntCapDay() {
        return tntCapDay;
    }

    public void setTntCapDay(int tntCapDay) {
        this.tntCapDay = normalizeActivationDay(tntCapDay);
        saveAction.run();
    }

    public boolean isFriendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    public void setFriendlyFireEnabled(boolean friendlyFireEnabled) {
        this.friendlyFireEnabled = friendlyFireEnabled;
    }

    public void toggleFriendlyFire() {
        this.friendlyFireEnabled = !this.friendlyFireEnabled;
        saveAction.run();
    }

    public boolean isHealthBelowNameEnabled() {
        return healthBelowNameEnabled;
    }

    public void setHealthBelowNameEnabled(boolean healthBelowNameEnabled) {
        this.healthBelowNameEnabled = healthBelowNameEnabled;
    }

    public void toggleHealthBelowName() {
        this.healthBelowNameEnabled = !this.healthBelowNameEnabled;
        saveAction.run();
    }

    public int getCavePlaceYLimit() {
        return cavePlaceYLimit;
    }

    public boolean isDayHudEnabled() {
        return dayHudEnabled;
    }

    public void setDayHudEnabled(boolean dayHudEnabled) {
        this.dayHudEnabled = dayHudEnabled;
    }

    public void toggleDayHud() {
        this.dayHudEnabled = !this.dayHudEnabled;
        saveAction.run();
    }

    public boolean isNightVisionEnabled() {
        return nightVisionEnabled;
    }

    public void setNightVisionEnabled(boolean nightVisionEnabled) {
        this.nightVisionEnabled = nightVisionEnabled;
        saveAction.run();
    }

    public void toggleNightVision() {
        this.nightVisionEnabled = !this.nightVisionEnabled;
        saveAction.run();
    }

    public void setCavePlaceYLimit(int cavePlaceYLimit) {
        this.cavePlaceYLimit = Math.max(0, cavePlaceYLimit);
        saveAction.run();
    }

    public boolean isEnderPearlEnabled() {
        return enderPearlEnabled;
    }

    public void setEnderPearlEnabled(boolean enderPearlEnabled) {
        this.enderPearlEnabled = enderPearlEnabled;
    }

    public void toggleEnderPearl() {
        this.enderPearlEnabled = !this.enderPearlEnabled;
        saveAction.run();
    }

    public boolean isChargedCreeperDropTntEnabled() {
        return chargedCreeperDropTntEnabled;
    }

    public void setChargedCreeperDropTntEnabled(boolean chargedCreeperDropTntEnabled) {
        this.chargedCreeperDropTntEnabled = chargedCreeperDropTntEnabled;
    }

    public int getChargedCreeperSpawnChance() {
        return chargedCreeperSpawnChance;
    }

    public void setChargedCreeperSpawnChance(int chargedCreeperSpawnChance) {
        this.chargedCreeperSpawnChance = Math.max(0, Math.min(100, chargedCreeperSpawnChance));
        saveAction.run();
    }

    public void toggleChargedCreeperDropTnt() {
        this.chargedCreeperDropTntEnabled = !this.chargedCreeperDropTntEnabled;
        saveAction.run();
    }

    public boolean isTntClimbEnabled() {
        return tntClimbEnabled;
    }

    public void setTntClimbEnabled(boolean tntClimbEnabled) {
        this.tntClimbEnabled = tntClimbEnabled;
    }

    public void toggleTntClimb() {
        this.tntClimbEnabled = !this.tntClimbEnabled;
        saveAction.run();
    }

    public boolean isBucketAssaultEnabled() {
        return bucketAssaultEnabled;
    }

    public boolean isCompassEnabled() {
        return compassEnabled;
    }

    public void setCompassEnabled(boolean compassEnabled) {
        this.compassEnabled = compassEnabled;
    }

    public void toggleCompass() {
        this.compassEnabled = !this.compassEnabled;
        saveAction.run();
    }

    public boolean isAutoPauseOnQuitEnabled() {
        return autoPauseOnQuitEnabled;
    }

    public void setAutoPauseOnQuitEnabled(boolean autoPauseOnQuitEnabled) {
        this.autoPauseOnQuitEnabled = autoPauseOnQuitEnabled;
    }

    public void toggleAutoPauseOnQuit() {
        this.autoPauseOnQuitEnabled = !this.autoPauseOnQuitEnabled;
        saveAction.run();
    }

    public void setBucketAssaultEnabled(boolean bucketAssaultEnabled) {
        this.bucketAssaultEnabled = bucketAssaultEnabled;
    }

    public void toggleBucketAssault() {
        this.bucketAssaultEnabled = !this.bucketAssaultEnabled;
        saveAction.run();
    }

    public boolean isNetherAssaultEnabled() {
        return netherAssaultEnabled;
    }

    public void setNetherAssaultEnabled(boolean netherAssaultEnabled) {
        this.netherAssaultEnabled = netherAssaultEnabled;
    }

    public void toggleNetherAssault() {
        this.netherAssaultEnabled = !this.netherAssaultEnabled;
        saveAction.run();
    }

    public boolean isPotionsEnabled() {
        return potionsEnabled;
    }

    public double getHungerConsumptionRate() {
        return hungerConsumptionRate;
    }

    public void setHungerConsumptionRate(double hungerConsumptionRate) {
        this.hungerConsumptionRate = Math.max(0.0D, hungerConsumptionRate);
        saveAction.run();
    }

    public void setPotionsEnabled(boolean potionsEnabled) {
        this.potionsEnabled = potionsEnabled;
    }

    public void togglePotions() {
        this.potionsEnabled = !this.potionsEnabled;
        saveAction.run();
    }

    public boolean isColoredNamesEnabled() {
        return coloredNamesEnabled;
    }

    public void setColoredNamesEnabled(boolean coloredNamesEnabled) {
        this.coloredNamesEnabled = coloredNamesEnabled;
        saveAction.run();
    }

    public void toggleColoredNames() {
        this.coloredNamesEnabled = !this.coloredNamesEnabled;
        saveAction.run();
    }

    public int getBaseCaptureTimeSeconds() {
        return baseCaptureTimeSeconds;
    }

    public void setBaseCaptureTimeSeconds(int baseCaptureTimeSeconds) {
        this.baseCaptureTimeSeconds = Math.max(1, baseCaptureTimeSeconds);
        saveAction.run();
    }

    public int getBaseCaptureRequiredPlayers() {
        return baseCaptureRequiredPlayers;
    }

    public void setBaseCaptureRequiredPlayers(int baseCaptureRequiredPlayers) {
        this.baseCaptureRequiredPlayers = Math.max(1, baseCaptureRequiredPlayers);
        saveAction.run();
    }

    public Integer getDeathLimit() {
        return deathLimit;
    }

    public void setDeathLimit(Integer deathLimit) {
        if (deathLimit == null) {
            this.deathLimit = null;
        } else {
            this.deathLimit = Math.max(1, deathLimit.intValue());
        }
        saveAction.run();
    }

    public Set<String> getGloballyAllowedPlaceBlocks() {
        return Collections.unmodifiableSet(globallyAllowedPlaceBlocks);
    }

    public void setGloballyAllowedPlaceBlocks(Set<String> allowed) {
        globallyAllowedPlaceBlocks.clear();
        if (allowed != null) {
            for (String blockKey : allowed) {
                if (blockKey != null && !blockKey.trim().isEmpty()) {
                    globallyAllowedPlaceBlocks.add(blockKey);
                }
            }
        }
        saveAction.run();
    }

    public boolean isGloballyAllowedPlaceBlock(Material material, byte data) {
        if (material == null) {
            return false;
        }
        String exactKey = toBlockKey(material, data);
        if (globallyAllowedPlaceBlocks.contains(exactKey)) {
            return true;
        }
        // Also check for material:0 to allow all data variants
        String baseKey = toBlockKey(material, (byte) 0);
        return globallyAllowedPlaceBlocks.contains(baseKey);
    }

    public String toBlockKey(Material material, byte data) {
        return material.name() + ":" + (data & 0xFF);
    }

    public ItemStack fromBlockKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        String[] parts = key.split(":");
        Material material = Material.getMaterial(parts[0]);
        if (material == null || !material.isBlock()) {
            return null;
        }

        short data = 0;
        if (parts.length > 1) {
            try {
                data = (short) Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                data = 0;
            }
        }

        return new ItemStack(material, 1, data);
    }

    public void clearGloballyAllowedPlaceBlocks() {
        globallyAllowedPlaceBlocks.clear();
        saveAction.run();
    }

    public List<ItemStack> getBaseLoadoutItems() {
        List<ItemStack> copy = new ArrayList<ItemStack>();
        for (ItemStack item : baseLoadoutItems) {
            if (item != null) {
                copy.add(item.clone());
            }
        }
        return Collections.unmodifiableList(copy);
    }

    public void setBaseLoadoutItems(List<ItemStack> items) {
        baseLoadoutItems.clear();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != null && item.getType() != Material.AIR) {
                    baseLoadoutItems.add(item.clone());
                }
            }
        }
        saveAction.run();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = Math.max(0, currentDay);
        saveAction.run();
    }

    public void addDay(int delta) {
        setCurrentDay(this.currentDay + delta);
    }

    private int normalizeActivationDay(int day) {
        if (day < 0) {
            return Integer.MAX_VALUE;
        }
        return day;
    }

    private boolean isEnabledAtDay(int activationDay) {
        return currentDay >= activationDay;
    }
}
