package org.example.listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.example.game.GameStateManager;
import org.example.settings.SettingsManager;
import org.example.team.TeamData;
import org.example.team.TeamManager;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

public class GameplayCapListener implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    private final Set<UUID> endPortalCooldown = new HashSet<UUID>();
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final SettingsManager settingsManager;
    private final GameStateManager gameStateManager;
    private final Map<UUID, Integer> deathCounts = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Double> hungerFractionCarry = new ConcurrentHashMap<UUID, Double>();
    private final Map<String, Location> netherPortalLinks = new ConcurrentHashMap<String, Location>();
    public Map<String, Location> getNetherPortalLinks() {
        return netherPortalLinks;
    }
    private Runnable saveAction = new Runnable() {
        @Override
        public void run() {
        }
    };

    public GameplayCapListener(JavaPlugin plugin, TeamManager teamManager, SettingsManager settingsManager, GameStateManager gameStateManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.settingsManager = settingsManager;
        this.gameStateManager = gameStateManager;
    }

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction == null ? new Runnable() {
            @Override
            public void run() {
            }
        } : saveAction;
    }

    public Map<UUID, Integer> getDeathCountsSnapshot() {
        return new java.util.HashMap<UUID, Integer>(deathCounts);
    }

    public void setDeathCounts(Map<UUID, Integer> loadedDeathCounts) {
        deathCounts.clear();
        if (loadedDeathCounts == null) {
            return;
        }

        for (Map.Entry<UUID, Integer> entry : loadedDeathCounts.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            deathCounts.put(entry.getKey(), Integer.valueOf(Math.max(0, entry.getValue().intValue())));
        }
    }

    @EventHandler
    public void onPortalCreate(final PortalCreateEvent event) {
        if (settingsManager.isNetherCapEnabled()) {
            return;
        }

        event.setCancelled(true);

        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                List<Block> blocks = event.getBlocks();
                for (Block block : blocks) {
                    if (block.getType() == Material.PORTAL) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block ->
                block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST);
    }

    private void createNetherPortalAt(Location center) {
        World world = center.getWorld();
        int px = center.getBlockX();
        int py = (int) center.getY();
        int pz = center.getBlockZ();

        for (int x = px - 1; x <= px + 2; x++) {
            world.getBlockAt(x, py - 1, pz).setType(Material.OBSIDIAN);
            world.getBlockAt(x, py + 3, pz).setType(Material.OBSIDIAN);
        }
        for (int y = py; y <= py + 2; y++) {
            world.getBlockAt(px - 1, y, pz).setType(Material.OBSIDIAN);
            world.getBlockAt(px + 2, y, pz).setType(Material.OBSIDIAN);
            world.getBlockAt(px,     y, pz).setType(Material.PORTAL);
            world.getBlockAt(px + 1, y, pz).setType(Material.PORTAL);
        }

        center.setX(px + 0.5);
        center.setY(py);
        center.setZ(pz + 0.5);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) return;

        String fromName = fromWorld.getName();

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (!settingsManager.isNetherCapEnabled()) {
                event.setCancelled(true);
                player.sendMessage(PREFIX + ChatColor.RED + "Nether portals are disabled.");
                return;
            }

            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                String netherName = fromName + "_nether";
                World nether = Bukkit.getWorld(netherName);
                if (nether == null) {
                    event.setCancelled(true);
                    player.sendMessage(PREFIX + ChatColor.RED + "Nether not available for this map.");
                    return;
                }

                event.setCancelled(true);
                Location from = event.getFrom();
                String linkKey = fromName + ":" + (from.getBlockX() >> 4) + ":" + (from.getBlockZ() >> 4);

                Location cached = netherPortalLinks.get(linkKey);
                if (cached != null && cached.getWorld() != null) {
                    if (!settingsManager.isNetherAssaultEnabled()
                            && teamManager.isLocationInsideEnemyBase(player, cached)) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Nether assault is disabled.");
                        return;
                    }
                    player.teleport(cached);
                    return;
                }

                double destX = from.getX() / 8.0;
                double destY = Math.max(64, Math.min(120, from.getY()));
                double destZ = from.getZ() / 8.0;
                final String finalLinkKey = linkKey;
                final World finalNether = nether;
                final Location fromClone = from.clone();

                int searchRadius = 128;
                int centerChunkX = ((int) destX) >> 4;
                int centerChunkZ = ((int) destZ) >> 4;
                int chunkRadius = (searchRadius >> 4) + 1;
                for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
                    for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                        finalNether.loadChunk(cx, cz, false);
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location linked = findNearestPortal(finalNether, destX, destY, destZ, searchRadius);
                    Location dest;
                    if (linked == null) {
                        dest = new Location(finalNether, destX, destY, destZ, fromClone.getYaw(), fromClone.getPitch());
                        createNetherPortalAt(dest);
                    } else {
                        dest = linked;
                    }
                    netherPortalLinks.put(finalLinkKey, dest.clone());
                    if (!settingsManager.isNetherAssaultEnabled()
                            && teamManager.isLocationInsideEnemyBase(player, dest)) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Nether assault is disabled.");
                        return;
                    }
                    player.teleport(dest);
                }, 5L);
                return;
            }

            if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                String overName = fromName.endsWith("_nether")
                        ? fromName.substring(0, fromName.length() - "_nether".length())
                        : "world";
                World over = Bukkit.getWorld(overName);
                if (over == null) {
                    event.setCancelled(true);
                    player.sendMessage(PREFIX + ChatColor.RED + "Overworld not available.");
                    return;
                }

                event.setCancelled(true);
                Location from = event.getFrom();
                String linkKey = fromName + ":" + (from.getBlockX() >> 4) + ":" + (from.getBlockZ() >> 4);

                Location cached = netherPortalLinks.get(linkKey);
                if (cached != null && cached.getWorld() != null) {
                    if (!settingsManager.isNetherAssaultEnabled()
                            && teamManager.isLocationInsideEnemyBase(player, cached)) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Nether assault is disabled.");
                        return;
                    }
                    player.teleport(cached);
                    return;
                }

                double destX = from.getX() * 8.0;
                double destY = Math.max(64, Math.min(255, from.getY()));
                double destZ = from.getZ() * 8.0;
                final String finalLinkKey = linkKey;
                final World finalOver = over;
                final Location fromClone = from.clone();

                int searchRadius = 256;
                int centerChunkX = ((int) destX) >> 4;
                int centerChunkZ = ((int) destZ) >> 4;
                int chunkRadius = (searchRadius >> 4) + 1;
                for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
                    for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                        finalOver.loadChunk(cx, cz, false);
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location linked = findNearestPortal(finalOver, destX, destY, destZ, searchRadius);
                    Location dest;
                    if (linked == null) {
                        dest = new Location(finalOver, destX, destY, destZ, fromClone.getYaw(), fromClone.getPitch());
                        createNetherPortalAt(dest);
                    } else {
                        dest = linked;
                    }
                    netherPortalLinks.put(finalLinkKey, dest.clone());
                    if (!settingsManager.isNetherAssaultEnabled()
                            && teamManager.isLocationInsideEnemyBase(player, dest)) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Nether assault is disabled.");
                        return;
                    }
                    player.teleport(dest);
                }, 5L);
                return;
            }
        }

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (!settingsManager.isEndCapEnabled()) {
                if (!endPortalCooldown.contains(player.getUniqueId())) {
                    endPortalCooldown.add(player.getUniqueId());
                    player.sendMessage(PREFIX + ChatColor.RED + "End portals are disabled.");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> endPortalCooldown.remove(player.getUniqueId()), 40L);
                }
                event.setCancelled(true);
                return;
            }

            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                String endName = fromName + "_the_end";
                World end = Bukkit.getWorld(endName);
                if (end == null) {
                    if (!endPortalCooldown.contains(player.getUniqueId())) {
                        endPortalCooldown.add(player.getUniqueId());
                        player.sendMessage(PREFIX + ChatColor.RED + "The End not available for this map.");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> endPortalCooldown.remove(player.getUniqueId()), 40L);
                    }
                    event.setCancelled(true);
                    return;
                }
                event.setCancelled(true);
                player.teleport(end.getSpawnLocation());
                return;
            }

            if (fromWorld.getEnvironment() == World.Environment.THE_END) {
                String overName = fromName.endsWith("_the_end")
                        ? fromName.substring(0, fromName.length() - "_the_end".length())
                        : "world";
                World over = Bukkit.getWorld(overName);
                if (over == null) {
                    if (!endPortalCooldown.contains(player.getUniqueId())) {
                        endPortalCooldown.add(player.getUniqueId());
                        player.sendMessage(PREFIX + ChatColor.RED + "Overworld not available.");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> endPortalCooldown.remove(player.getUniqueId()), 40L);
                    }
                    event.setCancelled(true);
                    return;
                }
                event.setCancelled(true);
                player.teleport(over.getSpawnLocation());
            }
        }
    }

    private Location findNearestPortal(World world, double cx, double cy, double cz, int searchRadius) {
        int icx = (int) cx;
        int icz = (int) cz;

        int minY = Math.max(1, (int) cy - 64);
        int maxY = Math.min(world.getMaxHeight() - 1, (int) cy + 64);

        Location nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = icx - searchRadius; x <= icx + searchRadius; x += 2) {
            for (int z = icz - searchRadius; z <= icz + searchRadius; z += 2) {
                double horizDist = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                if (horizDist >= nearestDist) continue;
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.PORTAL) {
                        continue;
                    }
                    if (horizDist < nearestDist) {
                        nearestDist = horizDist;
                        nearest = new Location(world, x + 0.5, y, z + 0.5);
                    }
                }
            }
        }

        return nearest;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = extractPlayerDamager(event.getDamager());
        if (attacker == null) {
            return;
        }

        if (gameStateManager == null || !gameStateManager.isRunning()) {
            event.setCancelled(true);
            return;
        }

        if (!settingsManager.isPvpCapEnabled()) {
            event.setCancelled(true);
            attacker.sendMessage(PREFIX + ChatColor.RED + "PvP is disabled.");
            return;
        }

        if (!settingsManager.isFriendlyFireEnabled()) {
            TeamData attackerTeam = teamManager.getTeamByPlayer(attacker.getUniqueId());
            TeamData victimTeam = teamManager.getTeamByPlayer(victim.getUniqueId());
            if (attackerTeam != null && victimTeam != null && attackerTeam.id == victimTeam.id) {
                event.setCancelled(true);
                attacker.sendMessage(PREFIX + ChatColor.RED + "Friendly fire is disabled.");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (!settingsManager.isTntCapEnabled() && event.getBlockPlaced().getType() == Material.TNT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "Assault TNT placement is disabled.");
            return;
        }

        if (event.getBlockPlaced().getType() == Material.TNT
                && !settingsManager.isTntClimbEnabled()
                && isStandingAbovePlacedBlock(event.getPlayer(), event.getBlockPlaced())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "TNT climbing is disabled.");

            Vector push = event.getPlayer().getLocation().getDirection().setY(0.0D);
            if (push.lengthSquared() < 0.0001D) {
                push = new Vector(1.0D, 0.0D, 0.0D);
            }
            event.getPlayer().setVelocity(push.normalize().multiply(0.55D).setY(0.12D));
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getBlock() == null || event.getBlock().getType() != Material.TNT) {
            return;
        }

        if (settingsManager.isTntCapEnabled()) {
            return;
        }

        event.setCancelled(true);
        if (event.getPlayer() != null) {
            event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "Assault TNT activation is disabled.");
        }
    }

    private boolean isStandingAbovePlacedBlock(Player player, Block block) {
        if (player == null || block == null) {
            return false;
        }

        org.bukkit.Location playerLocation = player.getLocation();
        int playerX = playerLocation.getBlockX();
        int playerZ = playerLocation.getBlockZ();
        int playerY = playerLocation.getBlockY();

        return playerX == block.getX()
                && playerZ == block.getZ()
                && (playerY == block.getY() + 1 || playerY == block.getY() + 2);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (settingsManager.isTntClimbEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Block blockUnderPlayer = event.getTo().clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
        if (blockUnderPlayer.getType() == Material.TNT) {
            Vector push = player.getLocation().toVector().subtract(blockUnderPlayer.getLocation().add(0.5D, 0.0D, 0.5D).toVector());
            push.setY(0.0D);
            if (push.lengthSquared() < 0.0001D) {
                push = player.getLocation().getDirection().setY(0.0D);
            }
            if (push.lengthSquared() < 0.0001D) {
                push = new Vector(1.0D, 0.0D, 0.0D);
            }
            player.setVelocity(push.normalize().multiply(0.55D).setY(0.22D));
        }
    }

    @EventHandler
    public void onPearlUse(PlayerInteractEvent event) {
        if (settingsManager.isEnderPearlEnabled()) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            String blockName = event.getClickedBlock().getType().name();
            if (blockName.equals("BED") || blockName.equals("BED_BLOCK") || blockName.endsWith("_BED")) {
                return;
            }
        }

        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "Ender pearl use is disabled.");
    }

    @EventHandler
    public void onPotionUse(PlayerInteractEvent event) {
        if (settingsManager.isPotionsEnabled()) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            String blockName = event.getClickedBlock().getType().name();
            if (blockName.equals("BED") || blockName.equals("BED_BLOCK") || blockName.endsWith("_BED")) {
                return;
            }
        }

        if (event.getItem() == null || event.getItem().getType() != Material.POTION) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "Potions are disabled.");
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (settingsManager.isPotionsEnabled()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        int oldFood = player.getFoodLevel();
        int newFood = event.getFoodLevel();

        if (gameStateManager == null || !gameStateManager.isRunning()) {
            hungerFractionCarry.remove(player.getUniqueId());
            event.setFoodLevel(oldFood);
            return;
        }

        if (newFood >= oldFood) {
            return;
        }

        double hungerRate = settingsManager.getHungerConsumptionRate();
        if (Math.abs(hungerRate - 1.0D) < 0.000001D) {
            return;
        }

        int consumed = oldFood - newFood;
        double carry = hungerFractionCarry.containsKey(player.getUniqueId())
                ? hungerFractionCarry.get(player.getUniqueId()).doubleValue() : 0.0D;
        double scaled = (consumed * hungerRate) + carry;
        int appliedLoss = (int) Math.floor(scaled);
        double newCarry = scaled - appliedLoss;

        if (appliedLoss < 0) {
            appliedLoss = 0;
            newCarry = 0.0D;
        }

        hungerFractionCarry.put(player.getUniqueId(), Double.valueOf(newCarry));
        event.setFoodLevel(Math.max(0, oldFood - appliedLoss));
    }

    @EventHandler
    public void onBucketAssault(PlayerBucketEmptyEvent event) {
        if (settingsManager.isBucketAssaultEnabled()) {
            return;
        }

        Material bucket = event.getBucket();
        if (bucket != Material.WATER_BUCKET && bucket != Material.LAVA_BUCKET) {
            return;
        }

        org.bukkit.Location placedLocation = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();

        if (bucket == Material.WATER_BUCKET
                && teamManager.isLocationWithinEnemyBaseDistance(event.getPlayer(), placedLocation, 10)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "You cannot place water within 10 blocks of an enemy base.");
            return;
        }

        if (!teamManager.isLocationInsideEnemyBase(event.getPlayer(), placedLocation)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "Bucket assault on enemy bases is disabled.");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!settingsManager.isChargedCreeperDropTntEnabled()) {
            return;
        }

        if (event.getEntityType() != EntityType.CREEPER) {
            return;
        }

        Creeper creeper = (Creeper) event.getEntity();
        if (!creeper.isPowered()) {
            return;
        }

        event.getDrops().add(new org.bukkit.inventory.ItemStack(Material.TNT, 1));
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event == null || event.getEntityType() != EntityType.CREEPER) {
            return;
        }

        int chance = settingsManager.getChargedCreeperSpawnChance();
        if (chance <= 0) {
            return;
        }

        if (chance >= 100 || Math.random() * 100.0D < chance) {
            Creeper creeper = (Creeper) event.getEntity();
            if (creeper != null && !creeper.isPowered()) {
                creeper.setPowered(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        UUID playerId = event.getEntity().getUniqueId();
        int deaths = deathCounts.containsKey(playerId) ? deathCounts.get(playerId).intValue() : 0;
        deaths++;
        deathCounts.put(playerId, Integer.valueOf(deaths));
        saveAction.run();

        Integer limit = settingsManager.getDeathLimit();
        if (limit == null) {
            return;
        }

        int remainingDeaths = Math.max(0, limit.intValue() - deaths);
        event.getEntity().sendMessage(PREFIX + ChatColor.YELLOW + "Deaths remaining: "
                + ChatColor.GOLD + remainingDeaths + ChatColor.YELLOW + ".");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Player respawnedPlayer = event.getPlayer();
        if (settingsManager.isNightVisionEnabled() && respawnedPlayer != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    applyNightVision(respawnedPlayer);
                }
            }, 1L);
        }

        Integer limit = settingsManager.getDeathLimit();
        if (limit == null) {
            return;
        }

        Integer deaths = deathCounts.get(event.getPlayer().getUniqueId());
        if (deaths == null || deaths.intValue() < limit.intValue()) {
            return;
        }

        final Player spectatorPlayer = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                spectatorPlayer.setGameMode(GameMode.SPECTATOR);
                spectatorPlayer.sendMessage(PREFIX + ChatColor.RED + "You reached the death limit and are now in spectator mode.");
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (settingsManager.isNightVisionEnabled()) {
            applyNightVision(event.getPlayer());
        }
    }

    private void applyNightVision(Player player) {
        if (player == null) {
            return;
        }
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, true, false), true);
    }

    private Player extractPlayerDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Arrow) {
            Arrow arrow = (Arrow) damager;
            if (arrow.getShooter() instanceof Player) {
                return (Player) arrow.getShooter();
            }
        }

        return null;
    }
}
