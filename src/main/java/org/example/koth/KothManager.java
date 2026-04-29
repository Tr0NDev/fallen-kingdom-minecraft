package org.example.koth;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class KothManager {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    private final JavaPlugin plugin;
    private final Random random = new Random();

    // KOTH settings
    private boolean enabled = false;
    private Location zoneCenter;
    private int zoneRadius = 0;
    private List<ItemStack> rewardItems = new ArrayList<>();
    private int rewardCount = 1;
    private int captureTimeSeconds = 32;
    private int startDay = 1;
    private int endDay = 10;
    private int scheduledKothDay = -1;

    // Runtime state
    private boolean active = false;
    private int currentDay = -1;
    private Player currentKing;
    private int kingTime = 0;
    private int lastDisplayedPercentage = -1;
    private BukkitTask kingTimer;
    private BukkitTask zoneCheckTimer;
    private final Map<UUID, Boolean> playersInZone = new ConcurrentHashMap<>();

    // Pending interactions
    private final Map<UUID, Integer> pendingRewardCount = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingCaptureTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingStartDay = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingEndDay = new ConcurrentHashMap<>();
    private final Map<UUID, Location> zoneCorner1 = new ConcurrentHashMap<>();
    private final Map<UUID, Location> zoneCorner2 = new ConcurrentHashMap<>();

    public KothManager(JavaPlugin plugin) {
        this.plugin = plugin;
        scheduleNextKothDay();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Location getZoneCenter() {
        return zoneCenter;
    }

    public void setZoneCenter(Location zoneCenter) {
        this.zoneCenter = zoneCenter;
    }

    public int getZoneRadius() {
        return zoneRadius;
    }

    public void setZoneRadius(int zoneRadius) {
        this.zoneRadius = zoneRadius;
    }

    public void setZone(Location center, int radius) {
        this.zoneCenter = center;
        this.zoneRadius = radius;
    }

    public List<ItemStack> getRewardItems() {
        return new ArrayList<>(rewardItems);
    }

    public void setRewardItems(List<ItemStack> rewardItems) {
        this.rewardItems = new ArrayList<>(rewardItems);
    }

    public int getRewardCount() {
        return rewardCount;
    }

    public void setRewardCount(int rewardCount) {
        this.rewardCount = rewardCount;
    }

    public int getCaptureTimeSeconds() {
        return captureTimeSeconds;
    }

    public void setCaptureTimeSeconds(int captureTimeSeconds) {
        this.captureTimeSeconds = captureTimeSeconds;
    }

    public int getStartDay() {
        return startDay;
    }

    public void setStartDay(int startDay) {
        this.startDay = startDay;
        scheduleNextKothDay();
    }

    public int getEndDay() {
        return endDay;
    }

    public void setEndDay(int endDay) {
        this.endDay = endDay;
        scheduleNextKothDay();
    }

    public int getScheduledKothDay() {
        return scheduledKothDay;
    }

    public void resetKothSchedule() {
        scheduleNextKothDay();
    }

    private void scheduleNextKothDay() {
        if (startDay <= endDay) {
            scheduledKothDay = random.nextInt(endDay - startDay + 1) + startDay;
        } else {
            scheduledKothDay = -1;
        }
    }

    public boolean isActive() {
        return active;
    }

    public Player getCurrentKing() {
        return currentKing;
    }

    public int getKingTime() {
        return kingTime;
    }

    public void forceStartKoth() {
        if (!enabled || active || zoneCenter == null || zoneRadius <= 0 || rewardItems.isEmpty()) {
            return;
        }
        startKoth();
    }

    public void startKothIfApplicable(int day) {
        if (!enabled || active || scheduledKothDay < 0) {
            return;
        }

        if (day == scheduledKothDay && zoneCenter != null && zoneRadius > 0 && !rewardItems.isEmpty()) {
            startKoth();
        }
    }

    private void startKoth() {
        active = true;
        currentKing = null;
        kingTime = 0;
        lastDisplayedPercentage = -1;
        scheduledKothDay = -1;

        // Announce KOTH start
        Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + "King of the Hill has started! Stay in the zone for " + captureTimeSeconds + " seconds to win!");

        // Start zone checking
        zoneCheckTimer = new BukkitRunnable() {
            @Override
            public void run() {
                checkZone();
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }

    private void checkZone() {
        if (!active || zoneCenter == null) {
            return;
        }

        // Check zone entry/exit messages
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean currentlyInZone = isInZone(player.getLocation());
            boolean wasInZone = playersInZone.getOrDefault(player.getUniqueId(), false);

            if (currentlyInZone && !wasInZone) {
                // Player entered the zone
                player.sendMessage(PREFIX + ChatColor.GREEN + "You entered the King of the Hill zone!");
            } else if (!currentlyInZone && wasInZone) {
                // Player left the zone
                player.sendMessage(PREFIX + ChatColor.RED + "You left the King of the Hill zone!");
            }

            playersInZone.put(player.getUniqueId(), currentlyInZone);
        }

        List<Player> playersInZone = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInZone(player.getLocation())) {
                playersInZone.add(player);
            }
        }

        if (playersInZone.size() == 1) {
            Player potentialKing = playersInZone.get(0);

            if (currentKing == null || !currentKing.equals(potentialKing)) {
                // New king or first king
                currentKing = potentialKing;
                kingTime = 0;
                lastDisplayedPercentage = -1;
                startKingTimer();
            }
        } else if (playersInZone.size() > 1) {
            // Multiple players in zone
            if (currentKing != null) {
                currentKing = null;
                kingTime = 0;
                lastDisplayedPercentage = -1;
                if (kingTimer != null) {
                    kingTimer.cancel();
                    kingTimer = null;
                }
            }
        } else {
            // No players in zone
            if (currentKing != null) {
                currentKing = null;
                kingTime = 0;
                lastDisplayedPercentage = -1;
                if (kingTimer != null) {
                    kingTimer.cancel();
                    kingTimer = null;
                }
            }
        }
    }

    private void startKingTimer() {
        if (kingTimer != null) {
            kingTimer.cancel();
        }

        kingTimer = new BukkitRunnable() {
            private int lastUpdatedKingTime = -1;

            @Override
            public void run() {
                kingTime++;
                
                // Only update ActionBar when kingTime changes (not every tick)
                if (kingTime != lastUpdatedKingTime) {
                    updateKothProgressBar();
                    lastUpdatedKingTime = kingTime;
                }
                
                if (kingTime >= captureTimeSeconds) {
                    winKoth();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }

    private void updateKothProgressBar() {
        if (currentKing == null) {
            return;
        }

        float progress = (float) kingTime / captureTimeSeconds;
        int percentage = (int) (progress * 100);

        // Only update if percentage changed
        if (percentage == lastDisplayedPercentage) {
            return;
        }
        lastDisplayedPercentage = percentage;

        int filledBars = (int) (progress * 20);

        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filledBars; i++) {
            bar.append("|");
        }
        bar.append(ChatColor.GRAY);
        for (int i = filledBars; i < 20; i++) {
            bar.append("|");
        }

        String message = "" + ChatColor.GOLD + percentage + "% " + bar.toString();

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBar(player, message);
        }
    }

    private void sendActionBar(Player player, String message) {
        if (player == null) {
            return;
        }

        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            String version = parts.length > 3 ? parts[3] : "";

            Class<?> serializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Method serializeMethod = serializerClass.getMethod("a", String.class);
            Object component = serializeMethod.invoke(null, "{\"text\":\"" + escapeJson(message) + "\"}");

            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Constructor<?> packetCtor = packetClass.getConstructor(
                    Class.forName("net.minecraft.server." + version + ".IChatBaseComponent"), byte.class);
            Object packet = packetCtor.newInstance(component, (byte) 2);

            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);
            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object handle = getHandle.invoke(craftPlayer);

            Field connectionField = handle.getClass().getField("playerConnection");
            Object connection = connectionField.get(handle);

            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacket.invoke(connection, packet);
        } catch (Throwable ignored) {
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void winKoth() {
        if (currentKing == null) {
            return;
        }

        // Give rewards
        List<ItemStack> availableRewards = new ArrayList<>(rewardItems);
        Collections.shuffle(availableRewards, random);

        for (int i = 0; i < Math.min(rewardCount, availableRewards.size()); i++) {
            ItemStack reward = availableRewards.get(i);
            if (reward != null) {
                currentKing.getInventory().addItem(reward.clone());
            }
        }

        Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + currentKing.getName() + " has won King of the Hill and received " + rewardCount + " reward(s)!");

        // Stop KOTH
        stopKoth();
    }

    public void stopKoth() {
        if (!active) {
            return;
        }

        active = false;
        currentKing = null;
        kingTime = 0;
        lastDisplayedPercentage = -1;
        playersInZone.clear();

        if (kingTimer != null) {
            kingTimer.cancel();
            kingTimer = null;
        }

        if (zoneCheckTimer != null) {
            zoneCheckTimer.cancel();
            zoneCheckTimer = null;
        }

        Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "King of the Hill has ended!");
    }

    private boolean isInZone(Location location) {
        if (zoneCenter == null || zoneRadius <= 0) {
            return false;
        }

        if (!location.getWorld().equals(zoneCenter.getWorld())) {
            return false;
        }

        double distance = location.distance(zoneCenter);
        return distance <= zoneRadius;
    }

    // Menu interaction methods
    public void openRewardInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bKOTH Rewards"));
        for (int i = 0; i < rewardItems.size() && i < 54; i++) {
            inv.setItem(i, rewardItems.get(i));
        }
        player.openInventory(inv);
    }

    public void saveRewardsFromInventory(Inventory inventory) {
        rewardItems.clear();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                rewardItems.add(item.clone());
            }
        }
    }

    // Pending interactions
    public void setPendingRewardCount(Player player, int count) {
        pendingRewardCount.put(player.getUniqueId(), count);
    }

    public Integer getPendingRewardCount(Player player) {
        return pendingRewardCount.remove(player.getUniqueId());
    }

    public void setPendingCaptureTime(Player player, int seconds) {
        pendingCaptureTime.put(player.getUniqueId(), seconds);
    }

    public Integer getPendingCaptureTime(Player player) {
        return pendingCaptureTime.remove(player.getUniqueId());
    }

    public void setPendingStartDay(Player player, int day) {
        pendingStartDay.put(player.getUniqueId(), day);
    }

    public Integer getPendingStartDay(Player player) {
        return pendingStartDay.remove(player.getUniqueId());
    }

    public void setPendingEndDay(Player player, int day) {
        pendingEndDay.put(player.getUniqueId(), day);
    }

    public Integer getPendingEndDay(Player player) {
        return pendingEndDay.remove(player.getUniqueId());
    }

    public void setZoneCorner1(Player player, Location location) {
        zoneCorner1.put(player.getUniqueId(), location);
    }

    public Location getZoneCorner1(Player player) {
        return zoneCorner1.get(player.getUniqueId());
    }

    public void setZoneCorner2(Player player, Location location) {
        zoneCorner2.put(player.getUniqueId(), location);
    }

    public Location getZoneCorner2(Player player) {
        return zoneCorner2.get(player.getUniqueId());
    }

    public void clearZoneCorners(Player player) {
        zoneCorner1.remove(player.getUniqueId());
        zoneCorner2.remove(player.getUniqueId());
    }

    public void calculateZoneFromCorners(Player player) {
        Location corner1 = zoneCorner1.remove(player.getUniqueId());
        Location corner2 = zoneCorner2.remove(player.getUniqueId());

        if (corner1 == null || corner2 == null) {
            return;
        }

        if (!corner1.getWorld().equals(corner2.getWorld())) {
            player.sendMessage(PREFIX + ChatColor.RED + "Corners must be in the same world!");
            return;
        }

        // Calculate center
        double centerX = (corner1.getX() + corner2.getX()) / 2;
        double centerY = (corner1.getY() + corner2.getY()) / 2;
        double centerZ = (corner1.getZ() + corner2.getZ()) / 2;

        Location center = new Location(corner1.getWorld(), centerX, centerY, centerZ);

        // Calculate radius (distance from center to corner)
        double radius = center.distance(corner1);

        setZoneCenter(center);
        setZoneRadius((int) Math.ceil(radius));

        player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH zone set! Center: " + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ() + " | Radius: " + zoneRadius);
    }
}