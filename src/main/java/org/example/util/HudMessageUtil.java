package org.example.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class HudMessageUtil {

    private static final String VERSION;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] parts = packageName.split("\\.");
        VERSION = parts.length > 3 ? parts[3] : "";
    }

    private HudMessageUtil() {
    }

    public static void sendActionBar(Player player, String message) {
        try {
            Object chatComponent = buildChatComponent(message);
            Class<?> packetClass = nmsClass("PacketPlayOutChat");
            Constructor<?> ctor = packetClass.getConstructor(nmsClass("IChatBaseComponent"), byte.class);
            Object packet = ctor.newInstance(chatComponent, (byte) 2);
            sendPacket(player, packet);
        } catch (Exception ignored) {
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> packetClass = nmsClass("PacketPlayOutTitle");
            Class<?> enumClass = nmsClass("PacketPlayOutTitle$EnumTitleAction");
            Class<?> componentClass = nmsClass("IChatBaseComponent");

            Object times = packetClass.getConstructor(int.class, int.class, int.class).newInstance(fadeIn, stay, fadeOut);
            sendPacket(player, times);

            Object titleEnum = Enum.valueOf((Class<? extends Enum>) enumClass, "TITLE");
            Object titlePacket = packetClass.getConstructor(enumClass, componentClass)
                    .newInstance(titleEnum, buildChatComponent(title));
            sendPacket(player, titlePacket);

            Object subtitleEnum = Enum.valueOf((Class<? extends Enum>) enumClass, "SUBTITLE");
            Object subtitlePacket = packetClass.getConstructor(enumClass, componentClass)
                    .newInstance(subtitleEnum, buildChatComponent(subtitle));
            sendPacket(player, subtitlePacket);
        } catch (Exception ignored) {
        }
    }

    private static Object buildChatComponent(String message) throws Exception {
        Class<?> serializerClass = nmsClass("IChatBaseComponent$ChatSerializer");
        Method method = serializerClass.getMethod("a", String.class);
        return method.invoke(null, "{\"text\":\"" + escapeJson(message) + "\"}");
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

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}