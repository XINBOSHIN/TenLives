package com.saikonohack.tenlives;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTables;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class TenLives extends JavaPlugin implements Listener {

    private final Set<Location> processedChests = new HashSet<>();
    private String deathAction;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        deathAction = getConfig().getString("death_action", "spectator");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TenLives plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TenLives plugin disabled!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealth != null) {
            double newMaxHealth = maxHealth.getBaseValue() - 2.0;
            if (newMaxHealth < 1.0) {
                if ("ban".equalsIgnoreCase(deathAction)) {
                    player.kickPlayer("Ваши жизни закончились...");
                    Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "Ваши жизни закончились...", null, null);
                } else if ("spectator".equalsIgnoreCase(deathAction)) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            } else {
                maxHealth.setBaseValue(newMaxHealth);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
            player.sendTitle("§cВы потеряли жизнь", "§eБудь внимательнее, они не вечны", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
        }, 1L);
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            ItemStack itemInOffHand = player.getInventory().getItemInOffHand();

            if (isValidTotem(itemInMainHand) || isValidTotem(itemInOffHand)) {
                AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHealth != null) {
                    double newMaxHealth = maxHealth.getBaseValue() + 2.0;
                    if (newMaxHealth > 20.0) {
                        newMaxHealth = 20.0;
                    }
                    maxHealth.setBaseValue(newMaxHealth);
                    player.sendTitle("§a1 Жизнь восстановлена", "§eПотрать её с умом", 10, 70, 20);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof StorageMinecart) {
            StorageMinecart minecart = (StorageMinecart) event.getRightClicked();
            Location loc = minecart.getLocation();

            if (processedChests.contains(loc)) {
                return;
            }

            processedChests.add(loc);
            Random random = new Random();
            if (random.nextDouble() < 0.1) {
                ItemStack totem = createCustomTotem();
                minecart.getInventory().addItem(totem);
//                getLogger().info("Тотем Жизни сгенерировался в вагонетке с сундуком!");
            }
//            else {
//                getLogger().info("Тотем Жизни не сгенерировался в вагонетке с сундуком.");
//            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gettotem")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack totem = createCustomTotem();
                player.getInventory().addItem(totem);
                player.sendMessage("Вы получили Тотем Жизни!");
                return true;
            } else if (sender instanceof ConsoleCommandSender) {
                sender.sendMessage("Эту команду может использовать только игрок.");
            }
        }
        return false;
    }

    private ItemStack createCustomTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        meta.setDisplayName("§6Тотем Жизни");
        meta.setCustomModelData(993);
        totem.setItemMeta(meta);
        return totem;
    }

    private boolean isValidTotem(ItemStack item) {
        if (item != null && item.getType() == Material.TOTEM_OF_UNDYING && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta.hasCustomModelData() && meta.getCustomModelData() == 993;
        }
        return false;
    }
}
