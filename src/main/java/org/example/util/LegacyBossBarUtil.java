package org.example.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class LegacyBossBarUtil {

    private static final String VERSION;
    private static final Map<UUID, Integer> ACTIVE_BAR_ENTITY_IDS = new ConcurrentHashMap<UUID, Integer>();

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] parts = packageName.split("\\.");
        VERSION = parts.length > 3 ? parts[3] : "";
    }

    private LegacyBossBarUtil() {
    }

    public static void show(Player player, String title, double progress) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            hide(player);

            Object wither = createWitherEntity(player.getWorld());
            float maxHealth = getEntityMaxHealth(wither);
            float health = (float) Math.max(1.0D, Math.min(maxHealth, maxHealth * clamp(progress, 0.0D, 1.0D)));

            setEntityCustomName(wither, title);
            setEntityCustomNameVisible(wither, true);
            setEntityInvisible(wither, true);
            setEntitySilent(wither, true);
            setEntityHealth(wither, health);

            Location location = getBossBarLocation(player);
            setEntityLocation(wither, location);

            Object packet = createSpawnPacket(wither);
            sendPacket(player, packet);

            int entityId = getEntityId(wither);
            ACTIVE_BAR_ENTITY_IDS.put(player.getUniqueId(), Integer.valueOf(entityId));
        } catch (Exception exception) {
            Bukkit.getLogger().warning("[FK] Failed to show legacy boss bar: " + exception.getClass().getSimpleName());
        }
    }

    public static void hide(Player player) {
        if (player == null) {
            return;
        }

        Integer entityId = ACTIVE_BAR_ENTITY_IDS.remove(player.getUniqueId());
        if (entityId == null || !player.isOnline()) {
            return;
        }

        try {
            Object destroyPacket = createDestroyPacket(entityId.intValue());
            sendPacket(player, destroyPacket);
        } catch (Exception exception) {
            Bukkit.getLogger().warning("[FK] Failed to hide legacy boss bar: " + exception.getClass().getSimpleName());
        }
    }

    public static void hideAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            hide(online);
        }
    }

    public static void hideAllExcept(Set<UUID> keep) {
        Set<UUID> snapshot = new HashSet<UUID>(ACTIVE_BAR_ENTITY_IDS.keySet());
        for (UUID playerId : snapshot) {
            if (keep != null && keep.contains(playerId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                hide(player);
            } else {
                ACTIVE_BAR_ENTITY_IDS.remove(playerId);
            }
        }
    }

    private static Object createWitherEntity(World world) throws Exception {
        Class<?> craftWorldClass = cbClass("CraftWorld");
        Object craftWorld = craftWorldClass.cast(world);
        Method getHandle = craftWorldClass.getMethod("getHandle");
        Object worldServer = getHandle.invoke(craftWorld);

        Class<?> entityWitherClass = nmsClass("EntityWither");
        Constructor<?> ctor = entityWitherClass.getConstructor(nmsClass("World"));
        return ctor.newInstance(worldServer);
    }

    private static Object createSpawnPacket(Object witherEntity) throws Exception {
        Class<?> packetClass = nmsClass("PacketPlayOutSpawnEntityLiving");
        Constructor<?> ctor = packetClass.getConstructor(nmsClass("EntityLiving"));
        return ctor.newInstance(witherEntity);
    }

    private static Object createDestroyPacket(int entityId) throws Exception {
        Class<?> packetClass = nmsClass("PacketPlayOutEntityDestroy");
        Constructor<?> ctor = packetClass.getConstructor(int[].class);
        Object idArray = Array.newInstance(int.class, 1);
        Array.setInt(idArray, 0, entityId);
        return ctor.newInstance(idArray);
    }

    private static void setEntityLocation(Object entity, Location location) throws Exception {
        Method setLocation = nmsClass("Entity").getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
        setLocation.invoke(entity, location.getX(), location.getY(), location.getZ(), 0.0F, 0.0F);
    }

    private static void setEntityCustomName(Object entity, String name) throws Exception {
        Method setCustomName = nmsClass("Entity").getMethod("setCustomName", String.class);
        setCustomName.invoke(entity, name == null ? "" : name);
    }

    private static void setEntityCustomNameVisible(Object entity, boolean visible) {
        try {
            Method setCustomNameVisible = nmsClass("Entity").getMethod("setCustomNameVisible", boolean.class);
            setCustomNameVisible.invoke(entity, Boolean.valueOf(visible));
        } catch (Exception ignored) {
        }
    }

    private static void setEntityInvisible(Object entity, boolean invisible) {
        try {
            Method setInvisible = nmsClass("Entity").getMethod("setInvisible", boolean.class);
            setInvisible.invoke(entity, Boolean.valueOf(invisible));
        } catch (Exception ignored) {
        }
    }

    private static void setEntitySilent(Object entity, boolean silent) {
        try {
            Method setSilent = nmsClass("Entity").getMethod("setSilent", boolean.class);
            setSilent.invoke(entity, Boolean.valueOf(silent));
        } catch (Exception ignored) {
        }
    }

    private static void setEntityHealth(Object entity, float health) throws Exception {
        Method setHealth = nmsClass("EntityLiving").getMethod("setHealth", float.class);
        setHealth.invoke(entity, Float.valueOf(health));
    }

    private static float getEntityMaxHealth(Object entity) throws Exception {
        Method getMaxHealth = nmsClass("EntityLiving").getMethod("getMaxHealth");
        Object value = getMaxHealth.invoke(entity);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 300.0F;
    }

    private static int getEntityId(Object entity) throws Exception {
        Method getId = nmsClass("Entity").getMethod("getId");
        Object value = getId.invoke(entity);
        return ((Integer) value).intValue();
    }

    private static void sendPacket(Player player, Object packet) throws Exception {
        Class<?> craftPlayerClass = cbClass("entity.CraftPlayer");
        Object craftPlayer = craftPlayerClass.cast(player);

        Method getHandle = craftPlayerClass.getMethod("getHandle");
        Object handle = getHandle.invoke(craftPlayer);

        Field connectionField = handle.getClass().getField("playerConnection");
        Object playerConnection = connectionField.get(handle);

        Method sendPacket = playerConnection.getClass().getMethod("sendPacket", nmsClass("Packet"));
        sendPacket.invoke(playerConnection, packet);
    }

    private static Class<?> nmsClass(String simpleName) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + VERSION + "." + simpleName);
    }

    private static Class<?> cbClass(String suffix) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + VERSION + "." + suffix);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Location getBossBarLocation(Player player) {
        Location base = player.getLocation().clone();
        Vector direction = base.getDirection();
        if (direction.lengthSquared() < 1.0E-6D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        }
        direction.normalize().multiply(20.0D);
        Location loc = base.add(direction);
        loc.setY(loc.getY() + 10.0D);
        return loc;
    }
}