package org.example.command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkinCommand implements CommandExecutor {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /skin <name>");
            return true;
        }

        String requestedName = args[0].trim();
        if (requestedName.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /skin <name>");
            return true;
        }

        SkinTexture texture = fetchSkinTexture(requestedName);
        if (texture == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Could not fetch skin for " + requestedName + ".");
            return true;
        }

        if (!applySkin(player, texture)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Failed to apply skin (server API limitation).");
            return true;
        }

        refreshAppearance(player);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Skin changed to " + ChatColor.YELLOW + requestedName + ChatColor.GREEN + ".");
        return true;
    }

    private void refreshAppearance(Player player) {
        sendPlayerInfoRefresh(player);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            online.hidePlayer(player);
            online.showPlayer(player);
        }
    }

    private boolean applySkin(Player player, SkinTexture texture) {
        try {
            Method getHandleMethod = player.getClass().getMethod("getHandle");
            Object handle = getHandleMethod.invoke(player);
            if (handle == null) {
                return false;
            }

            Method getProfileMethod = handle.getClass().getMethod("getProfile");
            Object profile = getProfileMethod.invoke(handle);
            if (profile == null) {
                return false;
            }

            Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
            Object properties = getPropertiesMethod.invoke(profile);

            Method removeAllMethod = properties.getClass().getMethod("removeAll", Object.class);
            Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);

            removeAllMethod.invoke(properties, "textures");

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class, String.class);
            Object property = propertyConstructor.newInstance("textures", texture.value, texture.signature);
            putMethod.invoke(properties, "textures", property);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void sendPlayerInfoRefresh(Player target) {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server.v1_8_R3.EntityPlayer");
            Class<?> packetClass = Class.forName("net.minecraft.server.v1_8_R3.Packet");
            Class<?> packetInfoClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo");
            Class<?> enumActionClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object targetHandle = getHandle.invoke(target);

            Object removeAction = Enum.valueOf((Class<Enum>) enumActionClass.asSubclass(Enum.class), "REMOVE_PLAYER");
            Object addAction = Enum.valueOf((Class<Enum>) enumActionClass.asSubclass(Enum.class), "ADD_PLAYER");

            Object handleArray = Array.newInstance(entityPlayerClass, 1);
            Array.set(handleArray, 0, targetHandle);

            Constructor<?> packetCtor = packetInfoClass.getConstructor(enumActionClass, handleArray.getClass());
            Object removePacket = packetCtor.newInstance(removeAction, handleArray);
            Object addPacket = packetCtor.newInstance(addAction, handleArray);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer == null || !viewer.isOnline()) {
                    continue;
                }

                Object viewerHandle = getHandle.invoke(viewer);
                Field connectionField = viewerHandle.getClass().getField("playerConnection");
                Object connection = connectionField.get(viewerHandle);
                Method sendPacket = connection.getClass().getMethod("sendPacket", packetClass);
                sendPacket.invoke(connection, removePacket);
                sendPacket.invoke(connection, addPacket);
            }

        } catch (Exception exception) {
            // Ignore packet refresh failures across forks.
        }
    }

    private SkinTexture fetchSkinTexture(String playerName) {
        try {
            String profileJson = httpGet("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            String uuid = extractField(profileJson, "id");
            if (uuid == null || uuid.isEmpty()) {
                return null;
            }

            String textureJson = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            String value = extractPropertyField(textureJson, "value");
            String signature = extractPropertyField(textureJson, "signature");
            if (value == null || signature == null) {
                return null;
            }

            return new SkinTexture(value, signature);
        } catch (Exception exception) {
            return null;
        }
    }

    private String httpGet(String endpoint) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractField(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private String extractPropertyField(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        Pattern propertyBlock = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"textures\\\".*?\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher matcher = propertyBlock.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static class SkinTexture {
        private final String value;
        private final String signature;

        private SkinTexture(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}
