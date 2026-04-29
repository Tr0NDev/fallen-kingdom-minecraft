package org.example.listener;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.example.stats.PlayerStatsManager;

public class PlayerStatsListener implements Listener {

    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,
            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.GLOWING_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.QUARTZ_ORE);

    private static final Set<EntityType> ANIMAL_TYPES = EnumSet.of(
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.COW,
            EntityType.CHICKEN,
            EntityType.SQUID,
            EntityType.WOLF,
            EntityType.MUSHROOM_COW,
            EntityType.OCELOT,
            EntityType.HORSE,
            EntityType.RABBIT,
            EntityType.BAT);

    private final PlayerStatsManager playerStatsManager;

    public PlayerStatsListener(PlayerStatsManager playerStatsManager) {
        this.playerStatsManager = playerStatsManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (victim != null) {
            playerStatsManager.addDeath(victim.getUniqueId());
        }

        Player killer = victim == null ? null : victim.getKiller();
        if (killer != null && victim != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            playerStatsManager.addKill(killer.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled() || event.getPlayer() == null || event.getBlock() == null) {
            return;
        }

        if (ORE_BLOCKS.contains(event.getBlock().getType())) {
            playerStatsManager.addOreMined(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (ANIMAL_TYPES.contains(event.getEntityType())) {
            playerStatsManager.addAnimalKilled(killer.getUniqueId());
        }
    }
}
