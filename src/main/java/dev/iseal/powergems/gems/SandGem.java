package dev.iseal.powergems.gems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dev.iseal.powergems.managers.Addons.CombatLogX.CombatLogXAddonManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev.iseal.powergems.PowerGems;
import dev.iseal.powergems.managers.SingletonManager;
import dev.iseal.powergems.managers.Configuration.GeneralConfigManager;
import dev.iseal.powergems.misc.Utils;
import dev.iseal.powergems.misc.AbstractClasses.Gem;
import dev.iseal.sealLib.Systems.I18N.I18N;

public class SandGem extends Gem {

    public SandGem() {
        super("Sand");
    }

    private final Utils utils = SingletonManager.getInstance().utils;
    private final GeneralConfigManager gcm = SingletonManager.getInstance().configManager.getRegisteredConfigInstance(GeneralConfigManager.class);

    @Override
    public void call(Action act, Player plr, ItemStack item) {
        caller = this.getClass();
        super.call(act, plr, item);
    }

    @Override
    protected void rightClick(Player plr, int level) {
        // Perform a raycast to find the target block
        Location eyeLocation = plr.getEyeLocation().clone();
        Location targetLocation = utils.getXBlocksInFrontOfPlayer(plr.getEyeLocation(), plr.getLocation().getDirection(), 100);

        utils.spawnLineParticles(
                eyeLocation,
                targetLocation,
                255,
                204,
                0,
                0.2D,
                (location) -> {
                    double radius = 1;
                    List<Entity> nearbyEntities = (List<Entity>) location.getWorld().getNearbyEntities(location, radius, radius, radius);
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof Player targetPlr && !entity.getUniqueId().equals(plr.getUniqueId())) {
                            if (targetPlr.getFoodLevel() > 6)
                                targetPlr.setFoodLevel(6);
                            targetPlr.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
                            if (PowerGems.isEnabled("CombatLogX") && gcm.isCombatLogXEnabled())
                                CombatLogXAddonManager.getInstance().setInFight(plr, targetPlr);
                        }
                    }
                },
                3
        );
    }

    @Override
    protected void leftClick(Player plr, int level) {
        // Perform a raycast to find the target block
        Location eyeLocation = plr.getEyeLocation().clone();
        Location targetLocation = utils.getXBlocksInFrontOfPlayer(plr.getEyeLocation(), plr.getLocation().getDirection(), 100);

        // Call the utils.drawFancyLine method
        utils.spawnFancyParticlesInLine(
                eyeLocation,
                targetLocation,
                255, 204, 0, // Semi dark yellow for line
                204, 153, 0, // Darker yellow for circles
                0.2, // Line interval
                5-level/2D, // Circle interval
                0.2, // Circle particle interval
                1+level/2D, // Circle radius
                loc -> {
                    double radius = 1;
                    List<Entity> nearbyEntities = (List<Entity>) loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof Player targetPlr && !entity.getUniqueId().equals(plr.getUniqueId())) {
                            targetPlr.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 1));
                            targetPlr.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0));
                            if (PowerGems.isEnabled("CombatLogX") && gcm.isCombatLogXEnabled())
                                CombatLogXAddonManager.getInstance().setInFight(plr, targetPlr);
                        }
                    }
                }, //line consumer
                loc -> {
                    double radius = 1 + level / 2D;
                    List<Entity> nearbyEntities = (List<Entity>) loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof Player targetPlr && !entity.getUniqueId().equals(plr.getUniqueId())) {
                            targetPlr.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 1));
                            targetPlr.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 1));
                        }
                    }
                },
                3
        );
    }

    @Override
    protected void shiftClick(Player plr, int level) {
        if (sm.sandMoveListen.hasToRemoveFrom(plr.getUniqueId())) {
            plr.sendMessage(I18N.translate("ALREADY_HAS_TRAP_ACTIVE"));
            return;
        }

        Location targetLocation = plr.getLocation().clone().add(0,-1,0);

        int tries = 0;
        while (gcm.isBlockedReplacingBlock(targetLocation.getBlock()) && tries < 70) {
            targetLocation.add(0, -1, 0);
            tries++;
        }

        HashMap<Block, Material> toReplace = new HashMap<>();

        utils.generateSquare(targetLocation, level*2).forEach(block -> {
            if (!gcm.isBlockedReplacingBlock(block)
                    && block.getRelative(BlockFace.UP).isEmpty()
                    && block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).isEmpty()
                    && !block.isEmpty() && !block.getRelative(BlockFace.DOWN).isEmpty() ) {

                Material oldMaterial = block.getType(); //Store the old blocks material
                sm.sandMoveListen.addToList(block, plr.getUniqueId());
                toReplace.put(block, oldMaterial);
            }
        });

        toReplace.forEach((block, material) -> {
            block.setType(Material.SAND);
        });

        sm.sandMoveListen.addToRemoveList(plr.getUniqueId(), toReplace);

        Bukkit.getScheduler().runTaskLater(PowerGems.getPlugin(), () -> {
            sm.sandMoveListen.removeFromList(plr.getUniqueId());
        }, 50L*level);
    }

    @Override
    public PotionEffectType getDefaultEffectType() {
        return PotionEffectType.FAST_DIGGING;
    }

    @Override
    public int getDefaultEffectLevel() {
        return 1;
    }

    @Override
    public ArrayList<String> getDefaultLore() {
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.GREEN + "Level %level%");
        lore.add(ChatColor.GREEN + "Abilities");
        lore.add(ChatColor.WHITE
                + "Right click: Weakens the target player, reducing their strength temporarily.");
        lore.add(ChatColor.WHITE
                + "Shift click: Engulfs the target player in darkness, impairing their vision and movement.");
        lore.add(ChatColor.WHITE + "Left click: Creates a sand block temporarily that slows enemies passing on it.");
        return lore;
    }

    @Override
    public Particle getDefaultParticle() {
        return Particle.FALLING_DUST;
    }
}
